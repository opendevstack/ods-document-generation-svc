package org.ods.doc.gen.leva.doc.services

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixtureWithComponent
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixtureWithTestData
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixturesOverall
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocDataFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocTestValidator
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.ProjectFixture
import org.ods.doc.gen.core.test.wiremock.WiremockManager
import org.ods.doc.gen.core.test.wiremock.WiremockServers
import org.ods.doc.gen.core.test.workspace.TestsReports
import org.ods.doc.gen.external.modules.git.BitbucketService
import org.ods.doc.gen.external.modules.jira.JiraService
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.project.data.Project
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject

/**
 * IMPORTANT: this test use Wiremock files to mock all the external interactions.
 *
 * ==>> HOW TO add more projects:
 *  In order to execute this against any project:
 *  1. Copy into src/test/resources/workspace/ID-project
 *      - metadata.yml: from Jenkins workspace
 *      - docs: from Jenkins workspace
 *      - xunit: from Jenkins workspace
 *      - ods-state: from Jenkins workspace
 *      - projectData: from Jenkins workspace (or release manager repo in BB)
 *  2. Add a 'release' component to metadata.yml (if not exist). Sample:
 *        - id: release
 *          name: /ID-project-release
 *          type: ods
 *  3. Update src/test/resources/leva-doc-functional-test-projects.yml
 *
 * ==>> HOW TO use record/play:
 *  We have 2 flags to play with the test:
 *  - RECORD: When TRUE wiremock will record the interaction with the servers and compare the pdf results with the expected
 *  - GENERATE_EXPECTED_PDF_FILES: When TRUE it will remove the expected pdfs and create a new ones
 *
 *  ie:
 *  - RECORD=false & GENERATE_EXPECTED_PDF_FILES=false are the default values. So then it can be executed everywhere.
 *  - RECORD=true & GENERATE_EXPECTED_PDF_FILES=false will record and compare the generate pdfs with the 'old' expected files
 *      ==> with this combination, if there's an error,
 *          we can compare new pdf with the old one, and see the implications of our changes in the pdfs
 *          see here _build/reports/LeVADocs_ the compared results images
 *  - RECORD=true & GENERATE_EXPECTED_PDF_FILES=true will record and generate new pdf expected files
 *
 */

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
class LevaDocServiceFunctTest extends Specification {

    private static final boolean RECORD = Boolean.parseBoolean(System.properties["testRecordMode"] as String)
    private static final boolean GENERATE_EXPECTED_PDF_FILES = Boolean.parseBoolean(System.properties["generateExpectedPdfFiles"] as String)

    @TempDir
    public File tempFolder

    @Inject
    LeVADocumentService leVADocumentService

    @Inject
    JiraService jiraService

    @Inject
    NexusService nexusService

    @Inject
    TestsReports testsReports

    @Inject
    Project project

    @Inject
    BitbucketService bitbucketService

    private WiremockManager jiraServer
    private WiremockManager docGenServer
    private WiremockManager nexusServer
    private WiremockManager sonarServer
    private WiremockManager bitbucketServer

    private LevaDocTestValidator testValidator
    private LevaDocDataFixture dataFixture

    def setupSpec(){
        new File(LevaDocTestValidator.SAVED_DOCUMENTS).mkdirs()
    }

    def setup() {
        String simpleName = this.class.simpleName
        dataFixture = new LevaDocDataFixture(simpleName, tempFolder, project, testsReports)
        testValidator = new LevaDocTestValidator(simpleName, tempFolder, project)
    }

    def cleanup() {
        docGenServer?.tearDown()
        jiraServer?.tearDown()
        nexusServer?.tearDown()
        sonarServer?.tearDown()
        bitbucketServer?.tearDown()
    }

    def "create #projectFixture.docType for project: #projectFixture.project"() {
        given: "A project data"
        copyProjectDataToTemporalFolder(projectFixture)
        setUpWireMock(projectFixture)
        Map data = dataFixture.buildFixtureData(projectFixture)
        setWorkSpaceOverridingCachedData(data)

        when: "the user creates a LeVA document"
        leVADocumentService."create${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        testValidator.validatePDF(GENERATE_EXPECTED_PDF_FILES, projectFixture)

        where: "Doctypes without testResults"
        projectFixture << new DocTypeProjectFixture().getProjects()
    }

    def "create #projectFixture.docType with tests results for project: #projectFixture.project"() {
        given: "A project data"
        copyProjectDataToTemporalFolder(projectFixture)
        setUpWireMock(projectFixture)
        Map data = dataFixture.buildFixtureData(projectFixture)
        ProjectData projectData = setWorkSpaceOverridingCachedData(data)
        data << testsReports.getAllResults(projectData, projectData.repositories)

        when: "the user creates a LeVA document"
        leVADocumentService."create${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        testValidator.validatePDF(GENERATE_EXPECTED_PDF_FILES, projectFixture)

        where: "Doctypes with tests results"
        projectFixture << new DocTypeProjectFixtureWithTestData().getProjects()
    }

    def "create #projectFixture.docType for component #projectFixture.component and project: #projectFixture.project"() {
        given: "A project data"
        copyProjectDataToTemporalFolder(projectFixture)
        setUpWireMock(projectFixture)
        Map data = dataFixture.buildFixtureData(projectFixture)
        setWorkSpaceOverridingCachedData(data)
        data.repo = dataFixture.getModuleData(projectFixture, data)

        when: "the user creates a LeVA document"
        leVADocumentService."create${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        testValidator.validatePDF(GENERATE_EXPECTED_PDF_FILES, projectFixture)

        where: "Doctypes with modules"
        projectFixture << new DocTypeProjectFixtureWithComponent().getProjects()
    }

    /**
     * When creating a new test for a project, this test depends on
     * @return
     */
    def "create Overall #projectFixture.docType for project: #projectFixture.project"() {
        given: "A project data"
        copyProjectDataToTemporalFolder(projectFixture)
        setUpWireMock(projectFixture)
        Map data = dataFixture.buildFixtureData(projectFixture)
        setWorkSpaceOverridingCachedData(data)
        dataFixture.updateExpectedComponentDocs(data, projectFixture)

        when: "the user creates a LeVA document"
        leVADocumentService."createOverall${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        testValidator.validatePDF(GENERATE_EXPECTED_PDF_FILES, projectFixture)

        where:
        projectFixture << new DocTypeProjectFixturesOverall().getProjects()
    }

    private Object copyProjectDataToTemporalFolder(ProjectFixture projectFixture) {
        FileUtils.copyDirectory(new File("src/test/resources/workspace/${projectFixture.project}"), tempFolder)
    }

    private void setUpWireMock(ProjectFixture projectFixture) {
        startUpWiremockServers(projectFixture)
        updateServicesWithWiremockConfig()
    }

    private ProjectData setWorkSpaceOverridingCachedData(Map<Object, Object> data) {
        // We need to override the value because of the cache in ProjectData
        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        projectData.data.env.WORKSPACE = tempFolder.absolutePath
        return projectData
    }

    private void startUpWiremockServers(ProjectFixture projectFixture) {
        String projectKey = projectFixture.project, doctype = projectFixture.docType
        log.info "Using PROJECT_KEY:${projectKey}"
        log.info "Using RECORD Wiremock:${RECORD}"
        log.info "Using GENERATE_EXPECTED_PDF_FILES:${GENERATE_EXPECTED_PDF_FILES}"
        log.info "Using temporal folder:${tempFolder.absolutePath}"

        String component = (projectFixture.component) ? "/${projectFixture.component}" : ""
        String scenarioPath = "${this.class.simpleName}/${projectKey}${component}/${doctype}/${projectFixture.version}"
        docGenServer = WiremockServers.DOC_GEN.build().withScenario(scenarioPath).startServer(RECORD)
        jiraServer = WiremockServers.JIRA.build().withScenario(scenarioPath).startServer(RECORD)
        nexusServer = WiremockServers.NEXUS.build().withScenario(scenarioPath).startServer(RECORD)
        bitbucketServer = WiremockServers.BITBUCKET.build().withScenario(scenarioPath).startServer(RECORD)
    }

    private void updateServicesWithWiremockConfig() {
        nexusService.baseURL = new URIBuilder(nexusServer.server().baseUrl()).build()
        jiraService.baseURL = new URIBuilder(jiraServer.server().baseUrl()).build()
        bitbucketService.baseURL = new URIBuilder(bitbucketServer.server().baseUrl()).build()
    }

}

