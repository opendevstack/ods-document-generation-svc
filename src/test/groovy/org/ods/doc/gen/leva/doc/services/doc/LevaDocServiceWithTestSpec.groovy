package org.ods.doc.gen.leva.doc.services.doc


import org.ods.doc.gen.leva.doc.fixture.DocTypeProjectFixtureBase
import org.ods.doc.gen.leva.doc.fixture.DocTypeProjectFixtureWithTestData
import org.ods.doc.gen.leva.doc.fixture.LevaDocTestValidator
import org.springframework.test.context.ActiveProfiles
import spock.lang.IgnoreIf
import spock.lang.Stepwise

/**
 * see LevaDocServiceTestBase
 */
@Stepwise
@ActiveProfiles(["test", "LevaDocServiceWithTestSpec"])
class LevaDocServiceWithTestSpec extends LevaDocServiceTestBase {

    @IgnoreIf({ data.projectFixture.project == DocTypeProjectFixtureBase.DUMMY_PROJECT })
    def "#projectFixture.docType with tests results - project: #projectFixture.project"() {
        given: "A project data"
        Map data = setUpFixture(projectFixture)
        prepareServiceDataParam(projectFixture, data)
        LevaDocTestValidator testValidator = new LevaDocTestValidator(tempFolder, projectFixture)
        String buildId = data.build.buildId

        when: "the user creates a LeVA document"
        leVADocumentService."create${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        assert testValidator.validatePDF(buildId), "see actual pdf:${testValidator.pdfDiffFileName(buildId)}"

        where: "Doctypes with tests results"
        projectFixture << new DocTypeProjectFixtureWithTestData().getProjects()
    }

}