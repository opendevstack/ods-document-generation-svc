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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.jooby.Env
import org.jooby.Jooby

class DocGen implements Jooby.Module {

    Config config
    Cache<String, Path> templatesCache
    DocumentTemplatesStore templatesStore

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

        this.templatesStore = new BitBucketDocumentTemplatesStore()
    }

    void configure(Env env, Config config, Binder binder) {
    }

    // Get document templates for a specific version
    private Path getTemplates(def version) {
        def path = templatesCache.getIfPresent(version)
        if (path == null) {
            path = templatesStore.getTemplatesForVersion(version, getPathForTemplatesVersion(version))
            templatesCache.put(version, path)
        }

        return path
    }

    // Generate a PDF document for a combination of template type, version and data
    def byte[] generate(String type, String version, Object data) {
        def result = []
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
            result = Util.convertHtmlToPDF(partials.document, partials.header, partials.footer, data)
        } catch (Throwable e) {
            throw e
        } finally {
            if (tmpDir) {
                FileUtils.deleteDirectory(tmpDir.toFile())
            }
        }

        return result
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
        static private def byte[] convertHtmlToPDF(Path documentHtmlFile, Path headerHtmlFile = null, Path footerHtmlFile = null, Object data) {
            def documentPDFFile = null

            try {
                documentPDFFile = Files.createTempFile("document", ".pdf")

                def cmd = ["wkhtmltopdf", "--encoding", "UTF-8", "--no-outline", "--print-media-type"]
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

                cmd.addAll(["--footer-center", "Page [page] of [topage]", "--footer-font-size", "10"])

                cmd << documentHtmlFile.toFile().absolutePath
                cmd << documentPDFFile.toFile().absolutePath

                println "[INFO]: executing cmd: ${cmd}"
                def result = Util.shell(cmd)
                if (result.rc != 0) {
                    println "[ERROR]: ${cmd} has exited with code ${result.rc}"
                    println "[ERROR]: ${result.stderr}"
                    throw new IllegalStateException(result.stderr)
                }

                return Files.readAllBytes(documentPDFFile)
            } catch (Throwable e) {
                throw e
            } finally {
                if (documentPDFFile) {
                    Files.delete(documentPDFFile)
                }
            }
        }

        // Execute a command in the shell
        static private def Map shell(List<String> cmd) {
            def proc = cmd.execute()
            proc.waitFor()

            return [
                rc: proc.exitValue(),
                stderr: proc.err.text,
                stdout: proc.in.text
            ]
        }
    }
}
