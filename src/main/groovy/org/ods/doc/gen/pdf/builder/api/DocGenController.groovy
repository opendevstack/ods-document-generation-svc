package org.ods.doc.gen.pdf.builder.api

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.ods.doc.gen.core.FileSystemHelper
import org.ods.doc.gen.pdf.builder.services.PdfGenerationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.inject.Inject
import javax.servlet.http.HttpServletResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson

@Slf4j
@RestController
@RequestMapping("/document")
class DocGenController {

    private static final RES_PREFIX = '{"data":"'.getBytes('US-ASCII')
    private static final RES_SUFFIX = '"}'.getBytes('US-ASCII')

    private PdfGenerationService pdfGeneration
    private final FileSystemHelper fileSystemHelper

    @Inject
    DocGenController(PdfGenerationService pdfGenerationService, FileSystemHelper fileSystemHelper) {
        this.fileSystemHelper = fileSystemHelper
        this.pdfGeneration = pdfGenerationService
    }

    @PostMapping
    void convertDocument(@RequestBody Map body, HttpServletResponse response) {
        validateRequestParams(body)
        logData(body)
        Path tmpDir
        try {
            tmpDir = fileSystemHelper.createTempDirectory("${body.metadata.type}-v${body.metadata.version}")
            Path pdf = convertToPdf(body, tmpDir)

            def dataLength = Files.size(pdf)
            response.setContentLength(RES_PREFIX.length + dataLength + RES_SUFFIX.length as int)
            def prefixIs = new ByteArrayInputStream(RES_PREFIX)
            def suffixIs = new ByteArrayInputStream(RES_SUFFIX)

            Files.newInputStream(pdf, StandardOpenOption.DELETE_ON_CLOSE).withStream { dataIs ->
                IOUtils.copy(new SequenceInputStream(Collections.enumeration([prefixIs, dataIs, suffixIs])), response.getOutputStream())
            }
        } finally {
            if (tmpDir) {
                FileUtils.deleteDirectory(tmpDir.toFile())
            }
        }
    }

    private Path convertToPdf(Map body, Path tmpDir) {
        try {
            Path documentPdf = pdfGeneration.generatePdfFile(body.metadata as Map, body.data as Map, tmpDir)
            Path tempFile = Files.createTempFile(tmpDir, 'temp', '.b64')
            tempFile.toFile().withOutputStream { os ->
                Base64.getEncoder().wrap(os).withStream { encOs ->
                    Files.copy(documentPdf, encOs)
                }
            }
            return tempFile
        } catch (Throwable e) {
            throw new RuntimeException("Conversion form HTML to PDF failed, corrupt data.", e)
        }
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