package org.ods.doc.gen.api

import groovy.util.logging.Slf4j
import org.ods.doc.gen.pdf.conversor.services.HtmlToPDFService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

import javax.inject.Inject
import java.nio.file.Files

@Slf4j
@RestController
class HealthController {

    private HtmlToPDFService htmlToPDFService

    @Inject
    HealthController(HtmlToPDFService htmlToPDFService){
        this.htmlToPDFService = htmlToPDFService
    }

    @GetMapping( "/health")
    Map check( ) {
        log.info("health executed")
        generatePdfData()
        Map result = [
                service: "docgen",
                status: "passing",
                time: new Date().toString()
        ]

        return result
    }

    private byte[] generatePdfData() {
        def documentHtmlFile = Files.createTempFile("document", ".html") << "<html>document</html>"

        def pdfBytesToString
        try {
            def documentPdf = htmlToPDFService.convert(documentHtmlFile)
            def data = Files.readAllBytes(documentPdf)
            if (!new String(data).startsWith("%PDF-1.4\n")) {
                throw new RuntimeException( "Conversion form HTML to PDF failed, corrupt data.")
            }
            pdfBytesToString = data.encodeBase64().toString()
        } catch (e) {
            throw new RuntimeException( "Conversion form HTML to PDF failed, corrupt data.", e)
        } finally {
            Files.delete(documentHtmlFile)
        }
        return pdfBytesToString
    }

}
