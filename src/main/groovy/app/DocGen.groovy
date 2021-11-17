package app

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.FileTemplateLoader
import com.google.inject.Binder
import com.google.inject.Inject
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import groovy.json.JsonOutput
import org.apache.commons.io.output.TeeOutputStream
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink

import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Duration

import org.apache.commons.io.FileUtils
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
                FileUtils.deleteDirectory(path.toFile())
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
    File generate(String type, String version, Object data) {
        File resultFile = null
        def tmpDir = null

        try {
            // Copy the templates directory including with any assets into a temporary location
            tmpDir = Files.createTempDirectory("${type}-v${version}")
            FileUtils.copyDirectory(getTemplates(version).toFile(), tmpDir.toFile())

            // Get partial templates from the temporary location and manipulate in-memory as needed
            def partials = getPartialTemplates(tmpDir, type, version) { name, template ->
                // Remove line separator and tab characters in partial template
                return template.replaceAll(System.getProperty("line.separator"), "").replaceAll("\t", "")
            }

            // Transform paths to partial templates to paths to rendered HTML files
            partials = partials.collectEntries { name, path ->
                // Write an .html file next to the .tmpl file containing the executed template
                def htmlFile = new File(FilenameUtils.removeExtension(path.toString()))
                htmlFile.setText(Util.executeTemplate(path, data))
                return [ name, htmlFile.toPath() ]
            }

            // Convert the exected templates into a PDF document
            resultFile = Util.convertHtmlToPDF(partials.document, partials.header, partials.footer, data)
        } catch (Throwable e) {
            throw e
        } finally {
            if (tmpDir) {
                FileUtils.deleteDirectory(tmpDir.toFile())
            }
        }

        return resultFile
    }

    // Read partial templates for a template type and version from the basePath directory
    private Map<String, Path> getPartialTemplates(Path basePath, String type, String version, Closure visitor) {
        def partials = [
            document: Paths.get(basePath.toString(), "templates", "${type}.html.tmpl"),
            header: Paths.get(basePath.toString(), "templates", "header.inc.html.tmpl"),
            footer: Paths.get(basePath.toString(), "templates", "footer.inc.html.tmpl")
        ]

        partials.each { name, path ->
            // Check if the partial template exists
            def file = path.toFile()
            if (!file.exists()) {
                throw new FileNotFoundException("could not find required template part '${name}' at '${path}'")
            }

            def template = file.text
            def templateNew = visitor(name, template)
            // Check if the template has been modified through the visitor
            if (template != templateNew) {
                // Write back the modified template contents
                file.text = templateNew
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
        static private def String executeTemplate(Path path, Object data) {
            // TODO: throw if template variables are not provided
            def loader = new FileTemplateLoader("", "")
            return new Handlebars(loader)
                .compile(path.toString())
                .apply(data)
        }

        // Convert a HTML document, with an optional header and footer, into a PDF
        static private File convertHtmlToPDF(Path documentHtmlFile, Path headerHtmlFile = null, Path footerHtmlFile = null, Object data) {
            def documentPDFFilePath = Files.createTempFile("document", ".pdf")

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

            cmd << documentHtmlFile.toFile().absolutePath
            cmd << documentPDFFilePath.toFile().absolutePath

            println "[INFO]: executing cmd: ${cmd}"

            def result = Util.shell(cmd)
            try{
                if (result.rc != 0) {
                    String stderr = result.stderr.text
                    println "[ERROR]: ${cmd} has exited with code ${result.rc}"
                    println "[ERROR]: ${stderr}"
                    throw new IllegalStateException(
                            "PDF Creation of ${documentHtmlFile} failed!\r:${stderr}\r:Error code:${result.rc}")
                }
            }finally{
                if(result!=null){
                    result.stderr.close()
                }
            }


            File documentPDFFile = documentPDFFilePath.toFile()
            fixDestinations(documentPDFFile)

            return documentPDFFile
        }

        // Execute a command in the shell
        static private def Map shell(List<String> cmd) {

            def proc = cmd.execute()
            Path tempFilePath = Files.createTempFile("shell", ".bin")
            File tempFile = tempFilePath.toFile()
            FileOutputStream tempFileOutputStream = new FileOutputStream(tempFile)
            def errOutputStream = new TeeOutputStream(tempFileOutputStream, System.err)

            try
            {
                proc.waitForProcessOutput(System.out, errOutputStream)
            }finally{
                tempFileOutputStream.close()
            }

            return [
                rc: proc.exitValue(),
                stderr: Files.newInputStream(tempFilePath, StandardOpenOption.DELETE_ON_CLOSE)
            ]
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
         * @param file a PDF file.
         */
        private static void fixDestinations(File file) {
            def doc = PDDocument.load(file)
            fixDestinations(doc)
            doc.save(file)
            doc.close()
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
            def catalog = doc.documentCatalog
            fixNamedDestinations(catalog, pages)
            fixOutline(catalog, pages)
            fixExplicitDestinations(pages)
        }

        private static fixNamedDestinations(catalog, pages) {
            fixStringDestinations(catalog.names?.dests, pages)
            fixNameDestinations(catalog.dests, pages)
        }

        private static fixStringDestinations(node, pages) {
            if (node) {
                node.names?.each { name, dest -> fixDestination(dest, pages) }
                node.kids?.each { fixStringDestinations(it, pages) }
            }
        }

        private static fixNameDestinations(dests, pages) {
            dests?.COSObject?.keySet()*.name.each { name ->
                def dest = dests.getDestination(name)
                if (dest in PDPageDestination) {
                    fixDestination(dest, pages)
                }
            }
        }

        private static fixOutline(catalog, pages) {
            def outline = catalog.documentOutline
            if (outline != null) {
                fixOutlineNode(outline, pages)
            }
        }

        private static fixOutlineNode(node, pages) {
            node.children().each { item ->
                fixDestinationOrAction(item, pages)
                fixOutlineNode(item, pages)
            }
        }

        private static fixExplicitDestinations(pages) {
            pages.each { page ->
                page.getAnnotations { it.subtype == PDAnnotationLink.SUB_TYPE }.each { link ->
                    fixDestinationOrAction(link, pages)
                }
            }
        }

        private static fixDestinationOrAction(item, pages) {
            def dest = item.destination
            if (dest == null && item.action?.subType == PDActionGoTo.SUB_TYPE) {
                dest = item.action.destination
            }
            if (dest in PDPageDestination) {
                fixDestination(dest, pages)
            }
        }

        private static fixDestination(dest, pages) {
            def pageNum = dest.pageNumber
            if (pageNum != -1) {
                dest.setPage(pages[pageNum])
            }
        }

    }
}
