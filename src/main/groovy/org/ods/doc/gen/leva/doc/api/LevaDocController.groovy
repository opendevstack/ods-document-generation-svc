package org.ods.doc.gen.leva.doc.api

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.leva.doc.services.LeVADocumentService
import org.springframework.web.bind.annotation.PathVariable
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
@RequestMapping("/levaDoc")
class LevaDocController {

    private LeVADocumentService leVADocumentService

    @Inject
    LevaDocController(LeVADocumentService leVADocumentUseCase){
        this.leVADocumentService = leVADocumentUseCase
    }

    @PostMapping("{projectId}/{build}/{levaDocType}")
    String buildDocument(
            @PathVariable("projectId") String projectId,
            @PathVariable("build") String build,
            @PathVariable("levaDocType") LevaDocType levaDocType,
            @RequestBody Map body){
        validateRequestParams(body)
        logData(body)
        return createDocument(levaDocType, body)
    }

    private String createDocument(LevaDocType levaDocType, Map data) {
        File tmpDir
        try {
            tmpDir = Files.createTempDirectory("${data.projectBuild}-${levaDocType}").toFile()
            data.env.WORKSPACE =  tmpDir.absolutePath
            return levaDocType.buildDocument.apply(leVADocumentService, data)
        } catch (Throwable e) {
            String msg = "Error building document:${levaDocType} with data:${data}"
            log.error(msg)
            throw new RuntimeException(msg, e)
        } finally {
            if (tmpDir) {
                FileUtils.deleteDirectory(tmpDir)
            }
        }
    }

    private static void validateRequestParams(Map body) {
        if (body.levaDocType == null) {
            throw new IllegalArgumentException("missing argument 'metadata.type'")
        }
        if (body.buildParams.projectKey == null) {
            throw new IllegalArgumentException("missing argument 'metadata.version'")
        }
        if (body?.data == null || 0 == body?.data.size()) {
            throw new IllegalArgumentException("missing argument 'data'")
        }
    }

    private static void logData(Map body) {
        log.info("buildDocument for: \n" +
                "- projectId:${body.buildParams.projectKey} \n" +
                "- build:${body.buildParams.buildNumber} \n" +
                "- levaDocType:${body.levaDocType}")
        if (log.isDebugEnabled()) {
            log.debug("Input request body data before send it to convert it to a pdf: ")
            log.debug(prettyPrint(toJson(body.data)))
        }
    }

}