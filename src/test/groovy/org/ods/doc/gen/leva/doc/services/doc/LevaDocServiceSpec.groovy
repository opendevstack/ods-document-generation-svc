package org.ods.doc.gen.leva.doc.services.doc

import org.ods.doc.gen.leva.doc.fixture.DocTypeProjectFixture
import org.ods.doc.gen.leva.doc.fixture.DocTypeProjectFixtureBase
import org.ods.doc.gen.leva.doc.fixture.LevaDocTestValidator
import org.ods.doc.gen.leva.doc.services.DocumentHistoryEntry
import org.ods.doc.gen.leva.doc.services.LeVADocumentService
import spock.lang.IgnoreIf

/**
 * see LevaDocServiceTestBase
 */
class LevaDocServiceSpec extends LevaDocServiceTestBase {

    @IgnoreIf({ data.projectFixture.project == DocTypeProjectFixtureBase.DUMMY_PROJECT })
    def "#projectFixture.docType - project: #projectFixture.project"() {
        given: "A project data"
        Map data = setUpFixture(projectFixture)
        prepareServiceDataParam(projectFixture, data)
        LevaDocTestValidator testValidator = new LevaDocTestValidator(tempFolder, projectFixture)
        String buildId = data.build.buildId

        when: "the user creates a LeVA document"
        List<DocumentHistoryEntry> history = leVADocumentService."create${projectFixture.docType}"(data)

        then: "the generated PDF is as expected"
        assert testValidator.validatePDF(buildId), "see actual pdf:${testValidator.pdfDiffFileName(buildId)}"

        where: "Doctypes without testResults"
        projectFixture << new DocTypeProjectFixture().getProjects()
    }

    def "getTestDescription"(testIssue, expected) {
        given:
        LeVADocumentService leVADocumentService = new LeVADocumentService(null, null, null,
                null, null, null, null)

        when:
        def result = leVADocumentService.getTestDescription(testIssue)

        then:
        result == expected

        where:
        testIssue                                      |       expected
        [name: '',description: '']                     |       'N/A'
        [name: 'NAME',description: '']                 |       'NAME'
        [name: '',description: 'DESCRIPTION']          |       'DESCRIPTION'
        [name: 'NAME',description: 'DESCRIPTION']      |       'DESCRIPTION'
    }
}

