package org.ods.doc.gen.controllers


import org.ods.doc.gen.pdf.conversor.HtmlToPDFService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

import javax.inject.Inject
import java.nio.file.Files

@RestController
class HealthController {

    private HtmlToPDFService htmlToPDFService

    @Inject
    HealthController(HtmlToPDFService htmlToPDFService){
        this.htmlToPDFService = htmlToPDFService
    }

    @GetMapping( "/health")
    Map check( ) {
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
