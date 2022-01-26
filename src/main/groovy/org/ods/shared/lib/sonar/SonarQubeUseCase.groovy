package org.ods.shared.lib.sonar

import groovy.util.logging.Slf4j
import org.ods.shared.lib.nexus.NexusService
import org.ods.doc.gen.project.data.Project
import org.springframework.stereotype.Service

import javax.inject.Inject

@SuppressWarnings(['JavaIoPackageAccess', 'EmptyCatchBlock'])
@Slf4j
@Service
class SonarQubeUseCase {

    private Project project
    private NexusService nexus

    @Inject
    SonarQubeUseCase(Project project, NexusService nexus) {
        this.project = project
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
