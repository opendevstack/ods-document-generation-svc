package app

import com.typesafe.config.ConfigFactory
import groovy.util.logging.Slf4j
import org.apache.groovy.json.internal.LazyMap
import org.jooby.Jooby
import org.jooby.MediaType
import org.jooby.json.Jackson

import java.nio.file.Files

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

            def body = req.body().to(LazyMap.class)

            validateRequestParams(body)

            if (log.isDebugEnabled()) {
                log.debug("Input request body data before send it to convert it to a pdf: ")
                log.debug(prettyPrint(toJson(body.data)))
            }

            def pdf = new DocGen().generate(body.metadata.type, body.metadata.version, body.data)
            rsp.send([
                data: pdf.encodeBase64().toString()
            ])
        })
        .consumes(MediaType.json)
        .produces(MediaType.json)

        get(this, "/health", { req, rsp ->
            def documentHtmlFile = Files.createTempFile("document", ".html") << "<html>document</html>"
            def headerHtmlFile = Files.createTempFile("header", ".html") << "<html>header</html>"
            def footerHtmlFile = Files.createTempFile("footer", ".html") << "<html>footer</html>"

            def message = null
            def status = "passing"
            def statusCode = 200

            def data
            try {
                data = DocGen.Util.convertHtmlToPDF(documentHtmlFile, headerHtmlFile, footerHtmlFile)
            } catch (e) {
                message = e.message
                status = "failing"
                statusCode = 500
            } finally {
                Files.delete(documentHtmlFile)
                Files.delete(headerHtmlFile)
                Files.delete(footerHtmlFile)
            }

            if (!new String(data).startsWith("%PDF-1.4\n")) {
                message = "conversion form HTML to PDF failed"
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
