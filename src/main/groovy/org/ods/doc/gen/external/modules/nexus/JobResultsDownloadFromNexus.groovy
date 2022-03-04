package org.ods.doc.gen.external.modules.nexus

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.ods.doc.gen.external.modules.nexus.NexusService

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class JobResultsDownloadFromNexus {

    NexusService nexusService

    JobResultsDownloadFromNexus(NexusService nexusService) {
        this.nexusService = nexusService
    }

    Path downloadTestsResults(Map data) {

        Path targetFolder = Files.createTempDirectory("testResultsJob_" + data.build.buildId)
        ZipParameters zipParameters = new ZipParameters()
        zipParameters.setDefaultFolderPath(targetFolder.toString())
        Map<String, String> testResultsURLs = data.build.testResultsURLs
        for (Map.Entry<String, String> testResultUrlPair : testResultsURLs.entrySet()) {
            Path filePath = downloadAndUncompress(testResultUrlPair.getKey(), testResultUrlPair.getValue(), targetFolder)
            ZipFile zipFile = new ZipFile(filePath.toString())
            zipFile.extractAll(targetFolder.toString(), zipParameters)
        }

        return targetFolder
    }

    private Path downloadAndUncompress(String key, String value, String targetFolder) {

        String nexusRepository = NexusService.DEFAULT_NEXUS_REPOSITORY
        int startsDirectoryName = value.indexOf(nexusRepository + "/")
        int startsArtifactName = value.lastIndexOf("/")

        String nexusDirectory = value.substring(startsDirectoryName +1, startsArtifactName)
        String artifactName = value.substring(startsArtifactName +1)

        nexusService.retrieveArtifact(nexusRepository, nexusDirectory, artifactName, targetFolder)

        return Paths.get(targetFolder, artifactName)
    }
}
