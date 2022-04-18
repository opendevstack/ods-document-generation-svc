package org.ods.doc.gen.leva.doc.api

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.ods.doc.gen.core.FileSystemHelper
import org.ods.doc.gen.leva.doc.services.DocumentHistoryEntry
import org.ods.doc.gen.leva.doc.services.LeVADocumentService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.inject.Inject

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson

@Slf4j
@RestController
@RequestMapping("/levaDoc")
class LevaDocController {

    private LeVADocumentService leVADocumentService
    private final FileSystemHelper fileSystemHelper

    @Inject
    LevaDocController(LeVADocumentService leVADocumentUseCase, FileSystemHelper fileSystemHelper){
        this.leVADocumentService = leVADocumentUseCase
        this.fileSystemHelper = fileSystemHelper
    }

    /**
     * The url identifies a project with a build that wants to generate a docType
     *
     * @param projectId
     * @param build
     * @param levaDocType
     * @param body
     * @return
     */
    @PostMapping("{projectId}/{build}/{levaDocType}")
    List<DocumentHistoryEntry> buildDocument(
            @PathVariable("projectId") String projectId,
            @PathVariable("build") String buildNumber,
            @PathVariable("levaDocType") LevaDocType levaDocType,
            @RequestBody Map body){
        validateRequestParams(body)
        logData(projectId, buildNumber, levaDocType, body)

        List<DocumentHistoryEntry> result = createDocument(projectId, buildNumber, levaDocType, body)
        if (log.isDebugEnabled()) {
            log.debug(prettyPrint(toJson(result)))
        }
        return result
    }

    @PostMapping("{projectId}/{build}/overall/{levaDocType}")
    void buildOverAllDocument(
            @PathVariable("projectId") String projectId,
            @PathVariable("build") String buildNumber,
            @PathVariable("levaDocType") LevaDocType levaDocType,
            @RequestBody Map body){
        validateRequestParams(body)
        logData(projectId, buildNumber, levaDocType, body)
        createDocument(projectId, buildNumber, levaDocType, body)
    }

    private List<DocumentHistoryEntry> createDocument(String projectId, String buildNumber, LevaDocType levaDocType, Map data) {
        try {
            prepareServiceDataParam(projectId, buildNumber, levaDocType, data)
            return levaDocType.buildDocument.apply(leVADocumentService, data)
        } catch (e) {
            if (log.isDebugEnabled()) {
                String msg = "Error building document: ${levaDocType} with data:${data}"
                log.debug(msg, e)
            }
            throw e
        }
    }

    private void prepareServiceDataParam(String projectId, String buildNumber, LevaDocType levaDocType, Map data) {
        data.documentType = levaDocType.toString()
        data.projectBuild = "${projectId}-${buildNumber}"
        data.projectId = projectId
        data.buildNumber = buildNumber
        data.tmpFolder = fileSystemHelper.createTempDirectory("${data.projectBuild}").toFile().absolutePath
    }

    private static void validateRequestParams(Map body) {
      /*  if (body.levaDocType == null) {
            throw new IllegalArgumentException("missing argument 'metadata.type'")
        }
        if (body.buildParams.projectKey == null) {
            throw new IllegalArgumentException("missing argument 'metadata.version'")
        }
        if (body?.data == null || 0 == body?.data.size()) {
            throw new IllegalArgumentException("missing argument 'data'")
        }*/
    }

    private static void logData(String projectId, String build, LevaDocType levaDocType, Map body) {
        log.info("buildDocument for: \n" +
                "- projectId:${projectId} \n" +
                "- build:${build} \n" +
                "- levaDocType:${levaDocType}")
        if (log.isDebugEnabled()) {
            log.debug(prettyPrint(toJson(body)))
        }
    }

}