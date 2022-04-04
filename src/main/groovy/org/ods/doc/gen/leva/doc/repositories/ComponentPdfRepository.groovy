package org.ods.doc.gen.leva.doc.repositories

import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.stereotype.Repository

@Repository
class ComponentPdfRepository {

    public static final String CONTENT_TPE = "application/zip"
    final NexusService nexusService

    ComponentPdfRepository(NexusService nexusService){
        this.nexusService = nexusService
    }

    String downloadDocument(ProjectData projectData, String documentName){
        String documentNamePdf = documentName.replaceFirst("zip", "pdf")
        String path = "${projectData.tmpFolder}/reports/${repo.id}"
        String jiraProjectKey = projectData.getJiraProjectKey()
        String version = projectData.build.version
        nexusService.downloadAndExtractZip(jiraProjectKey.toLowerCase(), version, path, documentName)
        return "${path}/${documentNamePdf}"
    }

    URI storeDocument(ProjectData projectData, String documentName, String pathToFile) {
        String directory = "${projectData.key.toLowerCase()}-${projectData.build.version}"
        return nexusService.storeArtifact(directory, documentName, pathToFile, CONTENT_TPE)
    }
}
