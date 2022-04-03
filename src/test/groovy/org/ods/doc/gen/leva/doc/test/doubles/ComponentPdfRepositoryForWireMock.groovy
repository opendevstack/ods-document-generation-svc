package org.ods.doc.gen.leva.doc.test.doubles

import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.leva.doc.repositories.ComponentPdfRepository
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Profile("test")
@Repository
class ComponentPdfRepositoryForWireMock extends ComponentPdfRepository{

    ComponentPdfRepositoryForWireMock(NexusService nexusService) {
        super(nexusService)
    }

    String downloadDocument(ProjectData projectData, String documentName){
        String documentNamePdf = documentName.replaceFirst("zip", "pdf")
        String path = "${projectData.tmpFolder}/reports/${repo.id}"
        // DTR-ORDGP-backend-WIP-2022-01-22_23-59-59.pdf
        // DTR-ORDGP-frontend-WIP-2022-01-22_23-59-59.pdf
        // TIR-ORDGP-backend-WIP-2022-01-22_23-59-59.pdf
        // TIR-ORDGP-frontend-WIP-2022-01-22_23-59-59.pdf
        return "${path}/${documentNamePdf}"
    }

    URI storeDocument(ProjectData projectData, String documentName, String pathToFile) {
        return new URIBuilder(
                "${nexusService.baseURL}" +
                "repository/${projectData.services.nexus.repository.name}/" +
             //   "${request.data.directory}/" +
                "${documentName}"
        ).build()
    }
}
