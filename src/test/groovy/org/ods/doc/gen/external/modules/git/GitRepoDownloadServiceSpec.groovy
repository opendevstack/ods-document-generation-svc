package org.ods.doc.gen.external.modules.git


import groovy.util.logging.Slf4j
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.external.modules.git.fixtureDatas.CheckRepoExists
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
class GitRepoDownloadServiceSpec extends Specification {

    @TempDir
    public File tmpFolder

    @Inject
    GitRepoDownloadService gitRepoDownloadService

    @Inject
    CheckRepoExists checkRepoExists

    def setup() {
        String simpleName = this.class.simpleName
    }

    def cleanup() {
    }

    def "test getRepoContentsToFolder() "() {
        given: "A project data"
        Map projectFixture = getProjectFixture()
        Map data = [:]
        data.build = buildJobParams(projectFixture)
        data.git =  buildGitData(projectFixture)
        data.openshift = [targetApiUrl:"https://openshift-sample"]

        when: "get a copy of the repository is called"
        checkRepoExists.checkRepoExists(projectFixture)
        String tmpFolderAbsolutePath = tmpFolder.getAbsolutePath()
        gitRepoDownloadService.getRepoContentsAsZipAndExtractToFolder(data, tmpFolderAbsolutePath)

        then: "check files are downloaded and no zip file remains there"
        log.info("Files in folder: ${tmpFolderAbsolutePath}")
        def dir = new File(tmpFolderAbsolutePath)
        boolean found = false
        dir.traverse() {
            log.info(it.getAbsolutePath())
            if ("Jenkinsfile" == it.getName()) {
                found = true
            }
        }
        log.info("End of Files in folder: ${tmpFolderAbsolutePath}")

        if (!found) {
            throw new Exception("Test was not able to find Jenkinsfile file.")
        }
    }

    def "test no repo url for getRepoContentsToFolder() "() {
        given: "A project data"
        Map projectFixture = getProjectFixture()
        Map data = [:]
        data.build = buildJobParams(projectFixture)
        data.git =  buildGitDataWithoutRepoUrl(projectFixture)
        data.openshift = [targetApiUrl:"https://openshift-sample"]

        when: "get a copy of the repository is called"
        checkRepoExists.checkRepoExists(projectFixture)
        String tmpFolderAbsolutePath = tmpFolder.getAbsolutePath()
        gitRepoDownloadService.getRepoContentsAsZipAndExtractToFolder(data, tmpFolderAbsolutePath)

        then: "check files are downloaded and no zip file remains there"
        def e = thrown(IllegalArgumentException)
        e.message == "Value for Git repoURL is empty or null."

    }

    def "test no repo release manager branch for getRepoContentsToFolder() "() {
        given: "A project data"
        Map projectFixture = getProjectFixture()
        Map data = [:]
        data.build = buildJobParams(projectFixture)
        data.git =  buildGitDataWithoutReleaseManagerBranch(projectFixture)
        data.openshift = [targetApiUrl:"https://openshift-sample"]

        when: "get a copy of the repository is called"
        checkRepoExists.checkRepoExists(projectFixture)
        String tmpFolderAbsolutePath = tmpFolder.getAbsolutePath()
        gitRepoDownloadService.getRepoContentsAsZipAndExtractToFolder(data, tmpFolderAbsolutePath)

        then: "check files are downloaded and no zip file remains there"
        def e = thrown(IllegalArgumentException)
        e.message == "Value for Git releaseManagerBranch is empty or null."

    }

    def "test checkRepositoryBranchExists() "() {
        given: "A project data"
        Map projectFixture = getProjectFixture()
        Map data = [:]
        data.build = buildJobParams(projectFixture)
        data.git =  buildGitData(projectFixture)
        data.openshift = [targetApiUrl:"https://openshift-sample"]

        when: "we try to see if repo branch exists"
        boolean result = gitRepoDownloadService.checkRepositoryBranchExists(data)

        then: "check files are downloaded and no zip file remains there"
        result == true
    }


    def "test gitCloneRepo() "() {
        given: "A project data"
        Map projectFixture = getProjectFixture()
        Map data = [:]
        data.build = buildJobParams(projectFixture)
        data.git =  buildGitData(projectFixture)
        data.openshift = [targetApiUrl:"https://openshift-sample"]

        Path tmpFolder = Files.createTempDirectory("repoContents")
        File tmpFolderFile = tmpFolder.toFile()

        when: "we try to checkoout repo"
        gitRepoDownloadService.getRepoContentsAsZipAndExtractToFolder(data, tmpFolderFile.getAbsolutePath())

        then: "check files are downloaded and no zip file remains there"
        boolean found = false
        tmpFolderFile.traverse() {
            log.info(it.getAbsolutePath())
            found = true
        }
        found == true
    }


    Map getProjectFixture() {
        Map projectFixture = [
                id: "ORDGP",
                releaseId: "1",
                version: "WIP",
                validation: "",
                releaseKey: "",
                releaseRepo: "ordgp-releasemanager",
        ]
        return projectFixture
    }

    private Map<String, String> buildJobParams(Map projectFixture){
        return  [
                targetEnvironment: "dev",
                targetEnvironmentToken: "D",
                version: "${projectFixture.version}",
                configItem: "BI-IT-DEVSTACK",
                changeDescription: "changeDescription",
                changeId: "changeId",
                rePromote: "false",
                releaseStatusJiraIssueKey: projectFixture.releaseKey,
                RUN_DISPLAY_URL : "",
                RELEASE_PARAM_VERSION : "3.0",
                BUILD_NUMBER : "666",
                BUILD_URL : "https://jenkins-sample",
                JOB_NAME : "${projectFixture.id}/${projectFixture.repo}-master"
                // "ofi2004-cd/ofi2004-cd-release-master",
        ]
    }

    private Map<String, String> buildGitData(Map projectFixture) {
        String bb_host = getBitBucketHost()
        return  [
                commit: "1e84b5100e09d9b6c5ea1b6c2ccee8957391beec",
                repoURL: "${bb_host}/${projectFixture.id}/${projectFixture.releaseRepo}.git",
                // "https://bitbucket/scm/ofi2004/ofi2004-release.git", //  new GitService().getOriginUrl()
                baseTag: "ods-generated-v3.0-3.0-0b11-D",
                targetTag: "ods-generated-v3.0-3.0-0b11-D",
                author: "s2o",
                message: "Swingin' The Bottle",
                time: "2021-04-20T14:58:31.042152",
                releaseManagerBranch: "refs/heads/master",
                //releaseManagerBranch: "refs/tags/CHG0066328",
        ]
    }

    private Map<String, String> buildGitDataWithoutRepoUrl(Map projectFixture) {
        return  [
                commit: "1e84b5100e09d9b6c5ea1b6c2ccee8957391beec",
                baseTag: "ods-generated-v3.0-3.0-0b11-D",
                targetTag: "ods-generated-v3.0-3.0-0b11-D",
                author: "s2o",
                message: "Swingin' The Bottle",
                time: "2021-04-20T14:58:31.042152",
        ]
    }

    private Map<String, String> buildGitDataWithoutReleaseManagerBranch(Map projectFixture) {
        String bb_host = getBitBucketHost()
        return  [
                commit: "1e84b5100e09d9b6c5ea1b6c2ccee8957391beec",
                repoURL: "${bb_host}/${projectFixture.id}/${projectFixture.releaseRepo}.git",
                baseTag: "ods-generated-v3.0-3.0-0b11-D",
                targetTag: "ods-generated-v3.0-3.0-0b11-D",
                author: "s2o",
                message: "Swingin' The Bottle",
                time: "2021-04-20T14:58:31.042152",
        ]
    }

    private String getBitBucketHost() {
        String value = System.getenv("BITBUCKET_URL")
        if (null == value) {
           value = System.properties["bitbucket.url"]
        }
        return value
    }
}
