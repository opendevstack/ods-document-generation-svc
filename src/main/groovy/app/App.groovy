package app

import com.typesafe.config.ConfigFactory
import groovy.util.logging.Slf4j
import org.jooby.Jooby
import org.jooby.MediaType
import org.jooby.json.Jackson
import util.DocUtils
import util.FileTools

import java.nio.file.Files
import java.nio.file.StandardOpenOption

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson
import static org.jooby.JoobyExtension.get
import static org.jooby.JoobyExtension.post

@Slf4j
class App extends Jooby {

    {

        use(new Jackson())
        use(new DocGen())

        post(this, "/document", { req, rsp ->

            Map body = req.body().to(HashMap.class)

            validateRequestParams(body)

            if (log.isDebugEnabled()) {
                log.debug("Input request body data before send it to convert it to a pdf: ")
                log.debug(prettyPrint(toJson(body.data)))
            }

            FileTools.newTempFile('document', '.b64') { dataFile ->
                new DocGen().generate(body.metadata.type, body.metadata.version, body.data).withFile { pdf ->
                    body = null // Not used anymore. Let it be garbage-collected.
                    dataFile.withOutputStream { os ->
                        Base64.getEncoder().wrap(os).withStream { encOs ->
                            Files.copy(pdf, encOs)
                        }
                    }
                }
                def dataLength = Files.size(dataFile)
                rsp.length(dataLength + RES_PREFIX.length + RES_SUFFIX.length)
                rsp.type(MediaType.json)
                def prefixIs = new ByteArrayInputStream(RES_PREFIX)
                def suffixIs = new ByteArrayInputStream(RES_SUFFIX)
                Files.newInputStream(dataFile, StandardOpenOption.DELETE_ON_CLOSE).initResource { dataIs ->
                    // Jooby is asynchronous. Upon return of the send method, the response has not necessarily
                    // been sent. For this reason, we rely on Jooby to close the InputStream and the temporary file
                    // will be deleted on close.
                    rsp.send(new SequenceInputStream(Collections.enumeration([prefixIs, dataIs, suffixIs])))
                }
            }

        })
        .consumes(MediaType.json)
        .produces(MediaType.json)

        get(this, "/health", { req, rsp ->
            def message = null
            def status = "passing"
            def statusCode = 200

            try {
                FileTools.withTempFile("document", ".html") { documentHtmlFile ->
                    documentHtmlFile  << "<html>document</html>"

                    DocGen.Util.convertHtmlToPDF(documentHtmlFile, null).withFile { pdf ->
                        def header = DocUtils.getPDFHeader(pdf)
                        if (header != PDF_HEADER) {
                            message = "conversion form HTML to PDF failed"
                            status = "failing"
                            statusCode = 500
                        }
                        return pdf
                    }

                }
            } catch (e) {
                message = e.message
                status = "failing"
                statusCode = 500
            }

            def result = [
                service: "docgen",
                status: status,
                time: new Date().toString()
            ]

            if (message) {
                result.message = message
            }

            rsp.status(statusCode).send(result)
        })
        .produces(MediaType.json)
    }

    private static final RES_PREFIX = '{"data":"'.getBytes('US-ASCII')
    private static final RES_SUFFIX = '"}'.getBytes('US-ASCII')
    private static final PDF_HEADER = '%PDF-1.4'

    private static void validateRequestParams(Map body) {
        if (body?.metadata?.type == null) {
            throw new IllegalArgumentException("missing argument 'metadata.type'")
        }

        if (body?.metadata?.version == null) {
            throw new IllegalArgumentException("missing argument 'metadata.version'")
        }

        if (body?.data == null) {
            throw new IllegalArgumentException("missing argument 'data'")
        }
    }

    static void main(String... args) {
        ConfigFactory.invalidateCaches()
        run(App.class, args)
    }
}
