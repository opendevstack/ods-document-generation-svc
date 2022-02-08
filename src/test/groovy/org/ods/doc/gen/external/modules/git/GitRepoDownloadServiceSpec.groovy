package org.ods.doc.gen.external.modules.git

import feign.Feign
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.auth.BasicAuthRequestInterceptor
import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocDataFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocTestValidator
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.ProjectFixture
import org.ods.doc.gen.external.modules.git.BitbucketService
import org.ods.doc.gen.external.modules.git.GitRepoDownloadService
import org.ods.doc.gen.external.modules.git.fixtureDatas.CheckRepoExists
import org.ods.doc.gen.leva.doc.services.StringCleanup
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject

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
        Map data = buildFixtureData(projectFixture)

        when: "get a copy of the repository is called"
        checkRepoExists.checkRepoExists(projectFixture)
        String tmpFolderAbsolutePath = tmpFolder.getAbsolutePath()
        gitRepoDownloadService.getRepoContentsToFolder(data, tmpFolderAbsolutePath)

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

    Map getProjectFixture() {
        Map projectFixture = [
                id: "FRML24113",
                releaseId: "1",
                version: "WIP",
                validation: "",
                releaseKey: "",
                releaseRepo: "frml24113-release",
        ]
        return projectFixture
    }
    Map buildFixtureData(Map projectFixture){


        Map data = [:]
        data.build = buildJobParams(projectFixture)
        data.git =  buildGitData(projectFixture)
        data.openshift = [targetApiUrl:"https://openshift-sample"]
        return data
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
        return  [
                commit: "1e84b5100e09d9b6c5ea1b6c2ccee8957391beec",
                url: "http://localhost:7990/${projectFixture.id}/${projectFixture.releaseRepo}",
                // "https://bitbucket/scm/ofi2004/ofi2004-release.git", //  new GitService().getOriginUrl()
                baseTag: "ods-generated-v3.0-3.0-0b11-D",
                targetTag: "ods-generated-v3.0-3.0-0b11-D",
                author: "s2o",
                message: "Swingin' The Bottle",
                time: "2021-04-20T14:58:31.042152",
                releaseManagerBranch: "refs/tags/CHG0066328",
        ]
    }



}
