package org.ods.doc.gen.pdf.builder.api


import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.core.FileSystemHelper
import org.ods.doc.gen.pdf.builder.services.PdfGenerationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson

@Slf4j
@RestController
@RequestMapping("/document")
class DocGenController {

    private PdfGenerationService pdfGeneration
    private final FileSystemHelper fileSystemHelper

    @Inject
    DocGenController(PdfGenerationService pdfGenerationService, FileSystemHelper fileSystemHelper){
        this.fileSystemHelper = fileSystemHelper
        this.pdfGeneration = pdfGenerationService
    }

    @PostMapping
    Map convertDocument(@RequestBody Map body){
        validateRequestParams(body)
        logData(body)
        return convertToPdf(body)
    }

    private Map<String, String> convertToPdf(Map body) {
        Path tmpDir
        String pdfBytesToString
        try {
            tmpDir = fileSystemHelper.createTempDirectory("${body.metadata.type}-v${body.metadata.version}")
            Path documentPdf = pdfGeneration.generatePdfFile(body.metadata as Map, body.data as Map, tmpDir)
            pdfBytesToString = Files.readAllBytes(documentPdf).encodeBase64().toString()
        } finally {
            if (tmpDir) {
                FileUtils.deleteDirectory(tmpDir.toFile())
            }
        }

        return [data: pdfBytesToString]
    }

    private static void logData(Map body) {
        if (log.isDebugEnabled()) {
            log.debug("Input request body data before send it to convert it to a pdf: ")
            log.debug(prettyPrint(toJson(body.data)))
        }
    }

    private static void validateRequestParams(Map body) {
        if (body?.metadata?.type == null) {
            throw new IllegalArgumentException("missing argument 'metadata.type'")
        }

        if (body?.metadata?.version == null) {
            throw new IllegalArgumentException("missing argument 'metadata.version'")
        }

        if (body?.data == null || 0 == body?.data.size()) {
            throw new IllegalArgumentException("missing argument 'data'")
        }
    }
}