package app

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.FileTemplateLoader
import com.google.inject.Binder
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.file.PathUtils
import org.apache.commons.io.output.TeeOutputStream
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentNameDestinationDictionary
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import util.FileTools

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Duration

import org.apache.commons.io.FilenameUtils
import org.jooby.Env
import org.jooby.Jooby

class DocGen implements Jooby.Module {

    Config config
    Cache<String, Path> templatesCache

    // TODO: use dependency injection
    DocGen() {
        this.config = ConfigFactory.load()

        this.templatesCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(1))
            .removalListener({ key, graph, cause ->
                def path = getPathForTemplatesVersion(key)
                if (Files.exists(path)) {
                    PathUtils.deleteDirectory(path)
                }
            })
            .build()
    }

    void configure(Env env, Config config, Binder binder) {
    }

    // Get document templates for a specific version
    private Path getTemplates(def version) {
        DocumentTemplatesStore store = new BitBucketDocumentTemplatesStore()
        if (!store.isApplicableToSystemConfig()) {
            store = new GithubDocumentTemplatesStore()
        } 
        println ("Using templates @${store.getZipArchiveDownloadURI(version)}")

        def path = templatesCache.getIfPresent(version)
        if (path == null) {
            path = store.getTemplatesForVersion(version, getPathForTemplatesVersion(version))
            templatesCache.put(version, path)
        }

        return path
    }

    // Generate a PDF document for a combination of template type, version and data
    Path generate(String type, String version, Object data) {
        // Copy the templates directory including with any assets into a temporary location
        return FileTools.withTempDir("${type}-v${version}") { tmpDir ->
            PathUtils.copyDirectory(getTemplates(version), tmpDir, StandardCopyOption.REPLACE_EXISTING)

            // Get partial templates from the temporary location and manipulate as needed
            def partials = getPartialTemplates(tmpDir, type)

            // Transform paths to partial templates to paths to rendered HTML files
            partials = partials.collectEntries { name, path ->
                // Write an .html file next to the .tmpl file containing the executed template
                def htmlFile = Paths.get(FilenameUtils.removeExtension(path.toString()))
                Util.executeTemplate(path, htmlFile, data)
                return [ name, htmlFile ]
            }

            // Convert the executed templates into a PDF document
            return Util.convertHtmlToPDF(partials.document, data)
        }
    }

    // Read partial templates for a template type and version from the basePath directory
    private static Map<String, Path> getPartialTemplates(Path basePath, String type) {
        def partials = [
            document: Paths.get(basePath.toString(), "templates", "${type}.html.tmpl"),
            header: Paths.get(basePath.toString(), "templates", "header.inc.html.tmpl"),
            footer: Paths.get(basePath.toString(), "templates", "footer.inc.html.tmpl")
        ]

        partials.each { name, path ->
            // Check if the partial template exists
            if (!Files.exists(path)) {
                throw new FileNotFoundException("could not find required template part '${name}' at '${path}'")
            }

            FileTools.newTempFile("${name}_tmpl") { tmp ->
                path.withReader { reader ->
                    tmp.withWriter { writer ->
                        reader.eachLine { line ->
                            def replaced = line.replaceAll('\t', '')
                            writer.write(replaced)
                        }
                    }
                }
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
            }
        }

        return partials
    }

    // Get a path to a directory holding document templates for a specific version
    private Path getPathForTemplatesVersion(String version) {
        return Paths.get(this.config.getString("application.documents.cache.basePath"), version)
    }

    class Util {
        // Execute a document template with the necessary data
        static private void executeTemplate(Path path, Path dest, Object data) {
            // TODO: throw if template variables are not provided
            def loader = new FileTemplateLoader("", "")
            dest.withWriter { writer ->
                new Handlebars(loader)
                        .compile(path.toString())
                        .apply(data, writer)
            }
        }

        // Convert a HTML document, with an optional header and footer, into a PDF
        static Path convertHtmlToPDF(Path documentHtmlFile, Object data) {
            def cmd = ["wkhtmltopdf", "--encoding", "UTF-8", "--no-outline", "--print-media-type"]
            cmd << "--enable-local-file-access"
            cmd.addAll(["-T", "40", "-R", "25", "-B", "25", "-L", "25"])

            if (data?.metadata?.header) {
                if (data.metadata.header.size() > 1) {
                    cmd.addAll(["--header-center", """${data.metadata.header[0]}
${data.metadata.header[1]}"""])
                } else {
                    cmd.addAll(["--header-center", data.metadata.header[0]])
                }

                cmd.addAll(["--header-font-size", "10", "--header-spacing", "10"])
            }

            cmd.addAll(["--footer-center", "'Page [page] of [topage]'", "--footer-font-size", "10"])

            if (data?.metadata?.orientation) {
                cmd.addAll(["--orientation", data.metadata.orientation])
            }

            cmd << documentHtmlFile.toAbsolutePath().toString()

            return FileTools.newTempFile("document", ".pdf") { documentPDFFile ->
                cmd << documentPDFFile.toAbsolutePath().toString()

                println "[INFO]: executing cmd: ${cmd}"

                def result = shell(cmd)
                if (result.rc != 0) {
                    println "[ERROR]: ${cmd} has exited with code ${result.rc}"
                    println "[ERROR]: ${result.stderr}"
                    throw new IllegalStateException(
                            "PDF Creation of ${documentHtmlFile} failed!\r:${result.stderr}\r:Error code:${result.rc}")
                }

                fixDestinations(documentPDFFile.toFile())
            }
        }

        // Execute a command in the shell
        static private Map shell(List<String> cmd) {

            def proc = cmd.execute()
            def stderr = null
            def rc = FileTools.withTempFile("shell", ".stderr") { tempFile ->
                tempFile.withOutputStream { tempFileOutputStream ->
                    new TeeOutputStream(System.err, tempFileOutputStream).withStream { errOutputStream ->
                        proc.waitForProcessOutput(System.out, errOutputStream)
                    }
                }
                def exitValue = proc.exitValue()
                if (exitValue) {
                    stderr = tempFile.text
                }
                return exitValue
            }

            return [
                rc: rc,
                stderr: stderr
            ]
        }

        private static final long MAX_MEMORY_TO_FIX_DESTINATIONS = 8192L

        /**
         * Fixes malformed PDF documents which use page numbers in local destinations, referencing the same document.
         * Page numbers should be used only for references to external documents.
         * These local destinations must use indirect page object references.
         * Note that these malformed references are not correctly renumbered when merging documents.
         * This method finds these malformed references and replaces the page numbers by the corresponding
         * page object references.
         * If the document is not malformed, this method will leave it unchanged.
         *
         * @param pdf a PDF file.
         */
        private static void fixDestinations(File pdf) {
            def memoryUsageSetting = MemoryUsageSetting.setupMixed(MAX_MEMORY_TO_FIX_DESTINATIONS)
            PDDocument.load(pdf, memoryUsageSetting).withCloseable { doc ->
                fixDestinations(doc)
                doc.save(pdf)
            }
        }

        /**
         * Fixes malformed PDF documents which use page numbers in local destinations, referencing the same document.
         * Page numbers should be used only for references to external documents.
         * These local destinations must use indirect page object references.
         * Note that these malformed references are not correctly renumbered when merging documents.
         * This method finds these malformed references and replaces the page numbers by the corresponding
         * page object references.
         * If the document is not malformed, this method will leave it unchanged.
         *
         * @param doc a PDF document.
         */
        private static void fixDestinations(PDDocument doc) {
            def pages = doc.pages as List // Accessing pages by index is slow. This will make it fast.
            fixExplicitDestinations(pages)
            def catalog = doc.documentCatalog
            fixNamedDestinations(catalog, pages)
            fixOutline(catalog, pages)
        }

        private static fixExplicitDestinations(pages) {
            pages.each { page ->
                page.getAnnotations { it instanceof PDAnnotationLink }.each { link ->
                    fixDestinationOrAction(link, pages)
                }
            }
        }

        private static fixNamedDestinations(catalog, pages) {
            fixStringDestinations(catalog.names?.dests, pages)
            fixNameDestinations(catalog.dests, pages)
        }

        private static fixOutline(catalog, pages) {
            def outline = catalog.documentOutline
            if (outline != null) {
                fixOutlineNode(outline, pages)
            }
        }

        private static fixStringDestinations(PDNameTreeNode<PDPageDestination> node, pages) {
            if (node) {
                node.names?.each { name, dest -> fixDestination(dest, pages) }
                node.kids?.each { fixStringDestinations(it, pages) }
            }
        }

        private static fixNameDestinations(PDDocumentNameDestinationDictionary dests, pages) {
            dests?.COSObject?.keySet()*.name.each { name ->
                def dest = dests.getDestination(name)
                if (dest instanceof PDPageDestination) {
                    fixDestination(dest, pages)
                }
            }
        }

        private static fixOutlineNode(PDOutlineNode node, pages) {
            node.children().each { item ->
                fixDestinationOrAction(item, pages)
                fixOutlineNode(item, pages)
            }
        }

        private static fixDestinationOrAction(item, pages) {
            def dest = item.destination
            if (dest == null) {
                def action = item.action
                if (action instanceof PDActionGoTo) {
                    dest = action.destination
                }
            }
            if (dest instanceof PDPageDestination) {
                fixDestination(dest, pages)
            }
        }

        private static fixDestination(PDPageDestination dest, List<PDPage> pages) {
            def pageNum = dest.pageNumber
            if (pageNum != -1) {
                dest.setPage(pages[pageNum])
            }
        }

    }
}
