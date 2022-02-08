package org.ods.doc.gen.leva.doc.services

import groovy.util.logging.Slf4j
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixtureWithComponent
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixtureWithTestData
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixturesOverall
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocDataFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocTestValidator
import org.ods.doc.gen.leva.doc.LevaDocWiremock
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.ProjectFixture
import org.ods.doc.gen.core.test.workspace.TestsReports
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
 *  - LevaDocWiremock.RECORD: When TRUE wiremock will record the interaction with the servers and compare the pdf results with the expected
 *  - LevaDocTestValidator.GENERATE_EXPECTED_PDF_FILES: When TRUE it will remove the expected pdfs and create a new ones
 *
 *  ie:
 *  - LevaDocWiremock.RECORD=false & GENERATE_EXPECTED_PDF_FILES=false are the default values. So then it can be executed everywhere.
 *  - LevaDocWiremock.RECORD=true & GENERATE_EXPECTED_PDF_FILES=false will record and compare the generate pdfs with the 'old' expected files
 *      ==> with this combination, if there's an error,
 *          we can compare new pdf with the old one, and see the implications of our changes in the pdfs
 *          see here _build/reports/LeVADocs_ the compared results images
 *  - LevaDocWiremock.RECORD=true & GENERATE_EXPECTED_PDF_FILES=true will record and generate new pdf expected files
 *
 */

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
class LevaDocServiceFunctTest extends Specification {
    
    @TempDir
    public File tempFolder

    @Inject
    LeVADocumentService leVADocumentService

    @Inject
    TestsReports testsReports

    @Inject
    Project project

    @Inject
    LevaDocWiremock levaDocWiremock

    private LevaDocTestValidator testValidator
    private LevaDocDataFixture dataFixture

    def setupSpec(){
        new File(LevaDocTestValidator.SAVED_DOCUMENTS).mkdirs()
    }

    def setup() {
        dataFixture = new LevaDocDataFixture(tempFolder, project, testsReports)
        testValidator = new LevaDocTestValidator(tempFolder, project)
    }

    def cleanup() {
        levaDocWiremock.tearDownWiremock()
    }

    def "create #projectFixture.docType for project: #projectFixture.project"() {
        given: "A project data"
        dataFixture.copyProjectDataToTemporalFolder(projectFixture)
        levaDocWiremock.setUpWireMock(projectFixture, tempFolder)
        Map data = dataFixture.buildFixtureData(projectFixture)
        prepareServiceDataParam(projectFixture, data)

        when: "the user creates a LeVA document"
        leVADocumentService."create${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        testValidator.validatePDF(projectFixture, data.build.buildId as String)

        where: "Doctypes without testResults"
        projectFixture << new DocTypeProjectFixture().getProjects()
    }

    def "create #projectFixture.docType with tests results for project: #projectFixture.project"() {
        given: "A project data"
        dataFixture.copyProjectDataToTemporalFolder(projectFixture)
        levaDocWiremock.setUpWireMock(projectFixture, tempFolder)
        Map data = dataFixture.buildFixtureData(projectFixture)
        ProjectData projectData = prepareServiceDataParam(projectFixture, data)
        data << testsReports.getAllResults(projectData, projectData.repositories)

        when: "the user creates a LeVA document"
        leVADocumentService."create${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        testValidator.validatePDF(projectFixture, data.build.buildId as String)

        where: "Doctypes with tests results"
        projectFixture << new DocTypeProjectFixtureWithTestData().getProjects()
    }

    def "create #projectFixture.docType for component #projectFixture.component and project: #projectFixture.project"() {
        given: "A project data"
        dataFixture.copyProjectDataToTemporalFolder(projectFixture)
        levaDocWiremock.setUpWireMock(projectFixture, tempFolder)
        Map data = dataFixture.buildFixtureData(projectFixture)
        prepareServiceDataParam(projectFixture, data)
        data.repo = dataFixture.getModuleData(projectFixture, data)

        when: "the user creates a LeVA document"
        leVADocumentService."create${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        testValidator.validatePDF(projectFixture, data.build.buildId as String)

        where: "Doctypes with modules"
        projectFixture << new DocTypeProjectFixtureWithComponent().getProjects()
    }

    /**
     * When creating a new test for a project, this test depends on
     * @return
     */
    def "create Overall #projectFixture.docType for project: #projectFixture.project"() {
        given: "A project data"
        dataFixture.copyProjectDataToTemporalFolder(projectFixture)
        levaDocWiremock.setUpWireMock(projectFixture, tempFolder)
        Map data = dataFixture.buildFixtureData(projectFixture)
        prepareServiceDataParam(projectFixture, data)
        dataFixture.updateExpectedComponentDocs(data, projectFixture)

        when: "the user creates a LeVA document"
        leVADocumentService."createOverall${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        testValidator.validatePDF(projectFixture, data.build.buildId as String)

        where:
        projectFixture << new DocTypeProjectFixturesOverall().getProjects()
    }

    private ProjectData prepareServiceDataParam(ProjectFixture projectFixture, Map<Object, Object> data) {
        data.tmpFolder = tempFolder.absolutePath
        data.documentType = projectFixture.docType
        data.projectBuild =  "${projectFixture.project}-1"
        data.buildNumber = "666"
        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        // We need to override the value because of the cache in ProjectData
        projectData.tmpFolder = tempFolder.absolutePath
        return projectData
    }

}

