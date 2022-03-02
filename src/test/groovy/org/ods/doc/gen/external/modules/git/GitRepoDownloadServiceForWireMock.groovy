package org.ods.doc.gen.external.modules.git


import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.external.modules.git.fixtureDatas.CheckRepoExists
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@ActiveProfiles("test")
@Service
class GitRepoDownloadServiceForWireMock extends GitRepoDownloadService {

    String [] VALID_PROJECTS = [ "ORDGP", "FRML24113" ]
    String PATH_TO_FILES="src/test/resources/workspace/"

    GitRepoDownloadServiceForWireMock(  @Value('${bitbucket.url}') String baseURL,
                                        @Value('${bitbucket.username}')  String username,
                                        @Value('${bitbucket.password}') String password,
                                        ZipFacade zipFacade) {
        super(baseURL, username, password, zipFacade)
    }

    @Override
    void getRepoContentsAsZipAndExtractToFolder(Map data, String targetFolderRelativePath) {
        log.info("GitRepoDownloadServiceForWireMock: getRepoContentsAsZipAndExtractToFolder")
        fakeGetReleaseManagerRepo(data, targetFolderRelativePath)
    }

    @Override
    void gitCloneReleaseManagerRepo(Map data, String targetFolderRelativePath) {
        log.info("GitRepoDownloadServiceForWireMock: gitCloneReleaseManagerRepo")
        fakeGetReleaseManagerRepo(data, targetFolderRelativePath)
    }

    private void fakeGetReleaseManagerRepo(Map data, String targetFolderRelativePath) {
        if (!checkRepositoryBranchExists(data)) {
            throw new RuntimeException("Repository not found.")
        }

        String repoURL = data.git.repoURL
        String releaseManagerBranch = data.git.releaseManagerBranch
        repoURL = repoURL.replaceFirst("\\.git", "")
        String [] urlPieces = repoURL.split('/')
        String project = urlPieces[urlPieces.length -2]
        String repo = urlPieces[urlPieces.length -1]

        File file = new File(PATH_TO_FILES);
        String absolutePath = file.getAbsolutePath();
        log.info(absolutePath)

        // + "/" + repo
        copyDirectory(absolutePath + "/" + project, targetFolderRelativePath)
    }

    @Override
    boolean checkRepositoryBranchExists(Map data) {
        log.info("GitRepoDownloadServiceForWireMock: checkRepositoryBranchExists")

        String repoURL = data.git.repoURL
        String releaseManagerBranch = data.git.releaseManagerBranch

        if (StringUtils.isEmpty(repoURL)) {
            logData(data);
            throw new IllegalArgumentException("Value for Git repoURL is empty or null.")
        }
        if (StringUtils.isEmpty(releaseManagerBranch)) {
            logData(data);
            throw new IllegalArgumentException("Value for Git releaseManagerBranch is empty or null.")
        }

        repoURL = repoURL.replaceFirst("\\.git", "")
        String [] urlPieces = repoURL.split('/')
        String project = urlPieces[urlPieces.length -2]
        String repo = urlPieces[urlPieces.length -1]

        for (String projectName : VALID_PROJECTS) {
            if (project == projectName) {
                return true
            }
        }
        return false
    }

    private boolean copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
            throws IOException {
        log.info("copyDirectory: ${sourceDirectoryLocation} -> ${destinationDirectoryLocation}")

        File tmp = new File(destinationDirectoryLocation)
        if (tmp.exists()) {
            tmp.deleteDir()
        }

        boolean someFailed = false
        Files.walk(Paths.get(sourceDirectoryLocation))
                .forEach(source -> {
                    Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                            .substring(sourceDirectoryLocation.length()));
                    try {
                        Files.copy(source, destination);
                    } catch (IOException e) {
                        e.printStackTrace();
                        someFailed = true
                    }
                });
        return someFailed
    }
}
