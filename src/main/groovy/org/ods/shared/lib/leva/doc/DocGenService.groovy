package org.ods.shared.lib.leva.doc


import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.pdf.conversor.services.PdfGenerationService
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

@Slf4j
@Service
class DocGenService {

    URI baseURL
    private final PdfGenerationService pdfGenerationService

    @Inject
    DocGenService(PdfGenerationService pdfGenerationService) {
       /* if (!baseURL?.trim()) {
            throw new IllegalArgumentException("Error: unable to connect to DocGen. 'baseURL' is undefined.")
        }

        try {
            this.baseURL = new URIBuilder(baseURL).build()
        } catch (e) {
            throw new IllegalArgumentException(
                "Error: unable to connect to DocGen. '${baseURL}' is not a valid URI."
            ).initCause(e)
        }*/
        this.pdfGenerationService = pdfGenerationService
    }

    
    byte[] createDocument(String type, String version, Map data) {
        def body = [
                metadata: [
                        type   : type,
                        version: version
                ],
                data    : data
        ]
        /*def response = Unirest.post("${this.baseURL}/document")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .body(JsonOutput.toJson(body))
            .asString()

        response.ifFailure {
            def message = "Error: unable to create document '${type} (v${version})'. " +
                "DocGen responded with code: '${response.getStatus()}' and message: '${response.getBody()}'."

            if (response.getStatus() == 404) {
                message = "Error: unable to create document '${type} (v${version})'. " +
                    "DocGen could not be found at: '${this.baseURL}'."
            }

            throw new RuntimeException(message)
        }

        def result = new JsonSlurperClassic().parseText(response.getBody())
        return decodeBase64(result.data)*/
        return convertToPdf(body)
    }

    private byte[] convertToPdf(Map body) {
        Path tmpDir
        String pdfBytesToString
        Path documentPdf
        try {
            tmpDir = Files.createTempDirectory("${body.metadata.type}-v${body.metadata.version}")
            documentPdf = pdfGenerationService.generatePdfFile(body.metadata as Map, body.data as Map, tmpDir)
        } catch (Throwable e) {
            throw new RuntimeException( "Conversion form HTML to PDF failed, corrupt data.", e)
        } finally {
            if (tmpDir) {
                FileUtils.deleteDirectory(tmpDir.toFile())
            }
        }

        return Files.readAllBytes(documentPdf)
    }
    
    private static byte[] decodeBase64(String base64String) {
        return Base64.decoder.decode(base64String)
    }
}
