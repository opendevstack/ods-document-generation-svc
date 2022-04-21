package org.ods.doc.gen.core.api

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.pdf.builder.services.HtmlToPDFService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

@Slf4j
@RestController
class HealthController {

    private HtmlToPDFService htmlToPDFService

    @Inject
    HealthController(HtmlToPDFService htmlToPDFService) {
        this.htmlToPDFService = htmlToPDFService
    }

    @GetMapping("/health")
    Map check() {
        log.info("health check to verify a pdf can be generated, executed")
        generatePdfData()
        Map result = [
                service: "docgen",
                status : "passing",
                time   : new Date().toString()
        ]

        return result
    }

    private void generatePdfData() {
        Path tmpDir = Files.createTempDirectory("generatePdfDataFolderTest")
        def documentHtmlFile = Files.createTempFile("document", ".html") << "<html>document</html>"

        try {
            def documentPdf = htmlToPDFService.convert(tmpDir, documentHtmlFile)
            def data = Files.readAllBytes(documentPdf)
            if (!new String(data).startsWith("%PDF-1.4\n")) {
                throw new RuntimeException("Conversion form HTML to PDF failed, corrupt data.")
            }
        } finally {
            Files.delete(documentHtmlFile)
            FileUtils.deleteDirectory(tmpDir.toFile())
        }
    }

}
