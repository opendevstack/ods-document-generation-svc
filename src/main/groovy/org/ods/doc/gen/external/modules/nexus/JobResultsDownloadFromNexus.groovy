package org.ods.doc.gen.external.modules.nexus

import groovy.util.logging.Slf4j
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.UnzipParameters
import net.lingala.zip4j.model.ZipParameters
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Service
class JobResultsDownloadFromNexus {

    private NexusService nexusService

    @Inject
    JobResultsDownloadFromNexus(NexusService nexusService) {
        this.nexusService = nexusService
    }

    void downloadTestsResults(Map<String, String> testResultsURLs, Path targetFolder) {

        for (Map.Entry<String, String> testResultUrlPair : testResultsURLs.entrySet()) {
            Path filePath = downloadAndUncompress(testResultUrlPair.getKey(), testResultUrlPair.getValue(), targetFolder.toString())
            ZipFile zipFile = new ZipFile(filePath.toString())
            zipFile.extractAll(targetFolder.toString())
        }
    }

    private Path downloadAndUncompress(String key, String value, String targetFolder) {

        String nexusRepository = NexusService.DEFAULT_NEXUS_REPOSITORY
        int startsDirectoryName = value.indexOf(nexusRepository + "/") + nexusRepository.length()
        int startsArtifactName = value.lastIndexOf("/")

        String nexusDirectory = value.substring(startsDirectoryName +1, startsArtifactName)
        String artifactName = value.substring(startsArtifactName +1)

        nexusService.retrieveArtifact(nexusRepository, nexusDirectory, artifactName, targetFolder)

        return Paths.get(targetFolder, artifactName)
    }
}
