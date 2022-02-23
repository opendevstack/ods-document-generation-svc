package org.ods.doc.gen.project.data

import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.ProjectFixture
import org.ods.doc.gen.core.test.wiremock.WiremockManager
import org.ods.doc.gen.core.test.wiremock.WiremockServers
import org.ods.doc.gen.core.test.workspace.TestsReports
import org.ods.doc.gen.external.modules.git.BitbucketService
import org.ods.doc.gen.external.modules.git.GitRepoDownloadService
import org.ods.doc.gen.external.modules.git.GitRepoVersionType
import org.ods.doc.gen.external.modules.jira.JiraService
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
class ProjectServiceSpec extends Specification {

    private static final boolean RECORD = Boolean.parseBoolean(System.properties["testRecordMode"] as String)
    private static final boolean GENERATE_EXPECTED_PDF_FILES = Boolean.parseBoolean(System.properties["generateExpectedPdfFiles"] as String)

    @Inject
    JiraService jiraService

    @Inject
    NexusService nexusService

    @Inject
    BitbucketService bitbucketService

    private WiremockManager jiraServer
    private WiremockManager nexusServer
    private WiremockManager sonarServer
    private WiremockManager bitbucketServer

    @TempDir
    public File tmpFolder

    @Inject
    Project project

    def setup() {
        String simpleName = this.class.simpleName
    }

    def cleanup() {
    }

    def "test getRepoContentsToFolder()"() {
        given: "A project data"
        Map projectFixture = getProjectFixture()
        Map data = buildFixtureData(projectFixture)
        setUpWireMock(projectFixture)

        when: "get a copy of the repository is called"
        project.getProjectData("1", data)
        then: "check files are downloaded and no zip file remains there"


        // where: "Doctypes without testResults"
        // projectFixture << new DocTypeProjectFixture().getProjects()
    }

    private void setUpWireMock(Map projectFixture) {
        startUpWiremockServers(projectFixture)
        updateServicesWithWiremockConfig()
    }

    private void startUpWiremockServers(Map projectFixture) {
        String projectKey = projectFixture.project
        String doctype = projectFixture.docType
        log.info "Using PROJECT_KEY:${projectKey}"
        log.info "Using RECORD Wiremock:${RECORD}"
        log.info "Using GENERATE_EXPECTED_PDF_FILES:${GENERATE_EXPECTED_PDF_FILES}"
        log.info "Using temporal folder:${tmpFolder.absolutePath}"

        String component = (projectFixture.component) ? "/${projectFixture.component}" : ""
        String scenarioPath = "${this.class.simpleName}/${projectKey}${component}/${doctype}/${projectFixture.version}"
        jiraServer = WiremockServers.JIRA.build().withScenario(scenarioPath).startServer(RECORD)
        nexusServer = WiremockServers.NEXUS.build().withScenario(scenarioPath).startServer(RECORD)
        bitbucketServer = WiremockServers.BITBUCKET.build().withScenario(scenarioPath).startServer(RECORD)
    }

    private void updateServicesWithWiremockConfig() {
        nexusService.baseURL = new URIBuilder(nexusServer.server().baseUrl()).build()
        jiraService.baseURL = new URIBuilder(jiraServer.server().baseUrl()).build()
        bitbucketService.baseURL = new URIBuilder(bitbucketServer.server().baseUrl()).build()
    }


    Map getProjectFixture() {
        Map projectFixture = [
                id: "FRML24113",
                project: "FRML24113",
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
        data.tmpFolder = tmpFolder.getAbsolutePath()
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
                JOB_NAME : "${projectFixture.id}/${projectFixture.repo}-master",
                // "ofi2004-cd/ofi2004-cd-release-master",
        ]
    }

    //TODO: add tag to project ORDGP
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
