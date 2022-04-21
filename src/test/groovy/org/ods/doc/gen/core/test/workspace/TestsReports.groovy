package org.ods.doc.gen.core.test.workspace

import groovy.util.logging.Slf4j
import org.ods.doc.gen.leva.doc.services.xunit.JUnitReportsService
import org.ods.doc.gen.project.data.ProjectData
import org.ods.doc.gen.project.data.TestType
import org.springframework.stereotype.Service
/**
 * Tests results should be at "${steps.env.WORKSPACE}/xunit"
 */
@Slf4j
@Service
class TestsReports {

    private final static List TYPES = [TestType.INSTALLATION, TestType.INTEGRATION, TestType.ACCEPTANCE, TestType.UNIT]
    private final JUnitReportsService jUnitTestReport
    static final String XUNIT_DOCUMENTS_BASE_DIR = 'xunit'

    TestsReports(JUnitReportsService jUnitTestReportsUseCase){
        this.jUnitTestReport = jUnitTestReportsUseCase
    }

    Map getAllResults(ProjectData projectData, List<Map> repos){
        Map data = [:]
        data.tests = [:]
        TYPES.each { type ->
            repos.each { repo ->
                data.tests << [(type.toLowerCase()): getResults(projectData, repo.id, type.toLowerCase())]
            }
        }
        return data
    }

    /**
     * see @org.ods.external.modules.orchestration.Stage#getTestResults
     */
    Map getResults(ProjectData projectData, String repoId, String type) {
        log.debug("Collecting JUnit XML Reports ('${type}') for ${repoId}")

        def testReportsPath = "${XUNIT_DOCUMENTS_BASE_DIR}/${repoId}/${type}"
        def testReportFiles = jUnitTestReport
                .loadTestReportsFromPath( "${projectData.tmpFolder}/${testReportsPath}")

        return [
            testReportFiles: testReportFiles,
            testResults: jUnitTestReport.parseTestReportFiles(testReportFiles),
        ]
    }

}
