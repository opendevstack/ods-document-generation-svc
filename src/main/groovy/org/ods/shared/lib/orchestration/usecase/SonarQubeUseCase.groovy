package org.ods.shared.lib.orchestration.usecase

import org.ods.shared.lib.util.IPipelineSteps
import  org.ods.shared.lib.orchestration.util.Project
import org.ods.shared.lib.services.NexusService

@SuppressWarnings(['JavaIoPackageAccess', 'EmptyCatchBlock'])
class SonarQubeUseCase {

    private Project project
    private NexusService nexus
    private IPipelineSteps steps

    SonarQubeUseCase(Project project, IPipelineSteps steps, nexus) {
        this.project = project
        this.steps = steps
        this.nexus = nexus
    }

    List<File> loadReportsFromPath(String path) {
        def result = []

        try {
            new File(path).traverse(nameFilter: ~/.*\.md$/, type: groovy.io.FileType.FILES) { file ->
                result << file
            }
        } catch (FileNotFoundException e) {}

        return result
    }

    String uploadReportToNexus(String version, Map repo, String type, File artifact) {
        return this.nexus.storeArtifactFromFile(
            this.project.services.nexus.repository.name,
            "${this.project.key.toLowerCase()}-${version}",
            "${type}-${repo.id}-${version}.md",
            artifact,
            "application/text"
        )
    }

}
