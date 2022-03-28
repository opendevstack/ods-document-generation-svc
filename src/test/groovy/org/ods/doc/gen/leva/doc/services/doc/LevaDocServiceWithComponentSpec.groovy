package org.ods.doc.gen.leva.doc.services.doc

import org.ods.doc.gen.leva.doc.fixture.DocTypeProjectFixtureBase
import org.ods.doc.gen.leva.doc.fixture.DocTypeProjectFixtureWithComponent
import org.ods.doc.gen.leva.doc.fixture.LevaDocTestValidator
import spock.lang.IgnoreIf

/**
 * see LevaDocServiceTestBase
 */
class LevaDocServiceWithComponentSpec extends LevaDocServiceTestBase {

    @IgnoreIf({ data.projectFixture.project == DocTypeProjectFixtureBase.DUMMY_PROJECT })
    def "#projectFixture.docType - component: #projectFixture.component - project: #projectFixture.project"() {
        given: "A project data"
        Map data = setUpFixture(projectFixture)
        prepareServiceDataParam(projectFixture, data)
        LevaDocTestValidator testValidator = new LevaDocTestValidator(tempFolder, projectFixture)
        String buildId = data.build.buildId
        data.repo = dataFixture.getModuleData(projectFixture)

        when: "the user creates a LeVA document"
        leVADocumentService."create${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        assert testValidator.validatePDF(buildId), "see actual pdf:${testValidator.pdfDiffFileName(buildId)}"

        where: "Doctypes with modules"
        projectFixture << new DocTypeProjectFixtureWithComponent().getProjects()
    }

}

