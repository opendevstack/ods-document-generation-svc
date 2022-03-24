package org.ods.doc.gen.leva.doc.services.doc

import org.ods.doc.gen.leva.doc.fixture.DocTypeProjectFixtureBase
import org.ods.doc.gen.leva.doc.fixture.DocTypeProjectFixturesOverall
import org.ods.doc.gen.leva.doc.fixture.LevaDocTestValidator
import org.ods.doc.gen.project.data.ProjectData
import spock.lang.IgnoreIf

/**
 * see LevaDocServiceTestBase
 */
class LevaDocServiceOverallSpec extends LevaDocServiceTestBase {

    // When recording mode, REMEMBER this test depends on LevaDocServiceWithComponentSpec
    @IgnoreIf({ data.projectFixture.project == DocTypeProjectFixtureBase.DUMMY_PROJECT })
    def "Overall #projectFixture.docType for project #projectFixture.project"() {
        given: "A project data"
        Map data = setUpFixture(projectFixture)
        prepareServiceDataParam(projectFixture, data)
        LevaDocTestValidator testValidator = new LevaDocTestValidator(tempFolder, projectFixture)
        String buildId = data.build.buildId
        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        dataFixture.updateExpectedComponentDocs(projectData, data, projectFixture)

        when: "the user creates a LeVA document"
        leVADocumentService."createOverall${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        assert testValidator.validatePDF(buildId), "see actual pdf:${testValidator.pdfDiffFileName(buildId)}"

        where:
        projectFixture << new DocTypeProjectFixturesOverall().getProjects()
    }

}

