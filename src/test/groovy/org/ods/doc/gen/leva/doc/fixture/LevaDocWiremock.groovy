package org.ods.doc.gen.leva.doc.fixture

import groovy.util.logging.Slf4j
import org.ods.doc.gen.core.test.wiremock.WiremockManager
import org.ods.doc.gen.core.test.wiremock.WiremockServers
import org.ods.doc.gen.pdf.builder.repository.WiremockDocumentRepository
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

import javax.inject.Inject

@Slf4j
@Component
class LevaDocWiremock {

    static final boolean GENERATE_EXPECTED_PDF_FILES = Boolean.parseBoolean(System.properties["generateExpectedPdfFiles"] as String)
    static final boolean RECORD = Boolean.parseBoolean(System.properties["testRecordMode"] as String)

    public static final String JIRA_URL = "jira.url"
    public static final String NEXUS_URL = "nexus.url"
    public static final String BB_URL = "bitbucket.url"

    private final Environment environment
    private final WiremockDocumentRepository wiremockDocumentRepository

    private WiremockManager jiraServer
    private WiremockManager nexusServer
    private WiremockManager bitbucketServer

    @Inject
    LevaDocWiremock(
                    WiremockDocumentRepository wiremockDocumentRepository,
                    Environment environment){
        this.wiremockDocumentRepository = wiremockDocumentRepository
        this.environment = environment
    }

    void setUpWireMock(ProjectFixture projectFixture, File tempFolder) {
        String projectKey = projectFixture.project, doctype = projectFixture.docType
        log.info "Using PROJECT_KEY:${projectKey}"
        log.info "Using RECORD Wiremock:${RECORD}"
        log.info "Using GENERATE_EXPECTED_PDF_FILES:${GENERATE_EXPECTED_PDF_FILES}"
        log.info "Using temporal folder:${tempFolder.absolutePath}"

        String component = (projectFixture.component) ? "/${projectFixture.component}" : ""
        String scenarioPath = "${projectKey}${component}/${doctype}/${projectFixture.version}"
        String jiraUrl = environment.getProperty(JIRA_URL)
        String nexusUrl = environment.getProperty(NEXUS_URL)
        String bbUrl = environment.getProperty(BB_URL)

        jiraServer = WiremockServers.JIRA.build(jiraUrl).withScenario(scenarioPath).startServer(RECORD)
        nexusServer = WiremockServers.NEXUS.build(nexusUrl).withScenario(scenarioPath).startServer(RECORD)
        bitbucketServer = WiremockServers.BITBUCKET.build(bbUrl).withScenario(scenarioPath).startServer(RECORD)
        wiremockDocumentRepository.setUpGithubRepository(projectFixture.templatesVersion)
    }

    void tearDownWiremock(){
        jiraServer?.tearDown()
        nexusServer?.tearDown()
        bitbucketServer?.tearDown()
        wiremockDocumentRepository.tearDownWiremock()
    }

}
