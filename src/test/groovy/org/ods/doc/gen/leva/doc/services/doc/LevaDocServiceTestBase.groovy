package org.ods.doc.gen.leva.doc.services.doc

import groovy.util.logging.Slf4j
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.test.workspace.TestsReports
import org.ods.doc.gen.leva.doc.fixture.LevaDocDataFixture
import org.ods.doc.gen.leva.doc.fixture.LevaDocTestValidator
import org.ods.doc.gen.leva.doc.fixture.LevaDocWiremock
import org.ods.doc.gen.leva.doc.fixture.LevaDocWiremockURLMapper
import org.ods.doc.gen.leva.doc.fixture.ProjectFixture
import org.ods.doc.gen.leva.doc.services.LeVADocumentService
import org.ods.doc.gen.project.data.Project
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ActiveProfiles(["test"])
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
class LevaDocServiceTestBase extends Specification {

    @Shared
    @TempDir
    File tempFolder

    @Inject
    LeVADocumentService leVADocumentService

    @Inject
    LevaDocWiremockURLMapper levaDocWiremockProxyData

    @Inject
    TestsReports testsReports

    @Inject
    Project project

    @Inject
    LevaDocWiremock levaDocWiremock

    LevaDocDataFixture dataFixture

    def setupSpec(){
        new File(LevaDocTestValidator.SAVED_DOCUMENTS).mkdirs()
    }

    def setup() {
        dataFixture = new LevaDocDataFixture(tempFolder, project, testsReports)
    }

    def cleanup() {
        levaDocWiremock.tearDownWiremock()
    }

    Map setUpFixture(ProjectFixture projectFixture) {
        log.info("setUpFixture: project:[{}], docType:[{}], overall:[{}]", projectFixture.project, projectFixture.docType, projectFixture.overall)
        levaDocWiremock.setUpWireMock(projectFixture, tempFolder)
        Map data = dataFixture.buildFixtureData(projectFixture)
        levaDocWiremockProxyData.updateURLs(levaDocWiremock)
        return data
    }

    void prepareServiceDataParam(ProjectFixture projectFixture, Map<Object, Object> data) {
        data.tmpFolder = tempFolder.getAbsolutePath()
        data.documentType = projectFixture.docType
        data.projectBuild =  "${projectFixture.project}-1"
        data.projectId = projectFixture.project
        data.buildNumber = "666"
    }
}