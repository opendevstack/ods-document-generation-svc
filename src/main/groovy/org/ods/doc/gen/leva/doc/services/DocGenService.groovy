package org.ods.doc.gen.leva.doc.services

import groovy.util.logging.Slf4j
import org.ods.doc.gen.pdf.builder.services.PdfGenerationService
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

@Slf4j
@Service
class DocGenService {

    private final PdfGenerationService pdfGenerationService

    @Inject
    DocGenService(PdfGenerationService pdfGenerationService) {
        this.pdfGenerationService = pdfGenerationService
    }

    Path createDocument(String type, String version, Map data) {
        def body = [
                metadata: [
                        type   : type,
                        version: version
                ],
                data    : data
        ]
        return convertToPdf(body)
    }

    private Path convertToPdf(Map body) {
        Path tmpDir
        Path documentPdf
        tmpDir = Files.createTempDirectory("${body.metadata.type}-v${body.metadata.version}")
        documentPdf = pdfGenerationService.generatePdfFile(body.metadata as Map, body.data as Map, tmpDir)

        return documentPdf
    }

}
