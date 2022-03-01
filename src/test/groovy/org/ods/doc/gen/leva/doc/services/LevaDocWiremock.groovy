package org.ods.doc.gen.leva.doc.services

import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.ProjectFixture
import org.ods.doc.gen.core.test.wiremock.WiremockManager
import org.ods.doc.gen.core.test.wiremock.WiremockServers
import org.ods.doc.gen.external.modules.git.BitbucketService
import org.ods.doc.gen.external.modules.jira.JiraService
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.springframework.stereotype.Component
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

import javax.inject.Inject

@Slf4j
@Component
class LevaDocWiremock {

    private static final boolean GENERATE_EXPECTED_PDF_FILES = Boolean.parseBoolean(System.properties["generateExpectedPdfFiles"] as String)
    private static final boolean RECORD = Boolean.parseBoolean(System.properties["testRecordMode"] as String)

    private final BitbucketService bitbucketService
    private final JiraService jiraService
    private final NexusService nexusService

    private WiremockManager jiraServer
    private WiremockManager nexusServer
    private WiremockManager sonarServer
    private WiremockManager bitbucketServer

    @Inject
    LevaDocWiremock(BitbucketService bitbucketService, JiraService jiraService, NexusService nexusService){
        this.bitbucketService = bitbucketService
        this.jiraService = jiraService
        this.nexusService = nexusService
    }

    void setUpWireMock(ProjectFixture projectFixture, File tempFolder) {
        startUpWiremockServers(projectFixture, tempFolder)
        updateServicesWithWiremockConfig()
    }

    void tearDownWiremock(){
        jiraServer?.tearDown()
        nexusServer?.tearDown()
        sonarServer?.tearDown()
        bitbucketServer?.tearDown()
    }

    private void startUpWiremockServers(ProjectFixture projectFixture, File tempFolder) {
        String projectKey = projectFixture.project, doctype = projectFixture.docType
        log.info "Using PROJECT_KEY:${projectKey}"
        log.info "Using RECORD Wiremock:${RECORD}"
        log.info "Using GENERATE_EXPECTED_PDF_FILES:${GENERATE_EXPECTED_PDF_FILES}"
        log.info "Using temporal folder:${tempFolder.absolutePath}"

        String component = (projectFixture.component) ? "/${projectFixture.component}" : ""
        String scenarioPath = "${projectKey}${component}/${doctype}/${projectFixture.version}"
        jiraServer = WiremockServers.JIRA.build().withScenario(scenarioPath).startServer(RECORD)
        nexusServer = WiremockServers.NEXUS.build().withScenario(scenarioPath).startServer(RECORD)
        bitbucketServer = WiremockServers.BITBUCKET.build().withScenario(scenarioPath).startServer(RECORD)
    }
    EnvironmentVariables env = new EnvironmentVariables()

    private void updateServicesWithWiremockConfig() {
        env.setup()
        nexusService.baseURL = new URIBuilder(nexusServer.server().baseUrl()).build()
        jiraService.baseURL = new URIBuilder(jiraServer.server().baseUrl()).build()
        //bitbucketService.baseURL = new URIBuilder(bitbucketServer.server().baseUrl()).build()
        env.set("BITBUCKET_URL",bitbucketServer.server().baseUrl())
    }

}
