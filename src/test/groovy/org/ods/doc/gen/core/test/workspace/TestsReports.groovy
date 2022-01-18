package org.ods.doc.gen.core.test.workspace

import groovy.util.logging.Slf4j
import  org.ods.shared.lib.xunit.JUnitTestReportsUseCase
import  org.ods.shared.lib.jenkins.PipelineUtil
import org.ods.shared.lib.project.data.TestType
import org.ods.shared.lib.jenkins.PipelineSteps
import org.springframework.stereotype.Service

/**
 * Tests results should be at "${steps.env.WORKSPACE}/xunit"
 */
@Slf4j
@Service
class TestsReports {
    private final static List TYPES = [TestType.INSTALLATION, TestType.INTEGRATION, TestType.ACCEPTANCE, TestType.UNIT]
    private final JUnitTestReportsUseCase jUnitTestReport
    private final PipelineSteps steps

    TestsReports(PipelineSteps steps, JUnitTestReportsUseCase jUnitTestReportsUseCase){
        this.steps = steps
        this.jUnitTestReport = jUnitTestReportsUseCase
    }

    Map getAllResults(List<Map> repos){
        Map data = [:]
        data.tests = [:]
        TYPES.each { type ->
            repos.each { repo ->
                data.tests << [(type.toLowerCase()): getResults(repo.id, type.toLowerCase())]
            }
        }
        return data
    }

    /**
     * see @org.ods.shared.lib.orchestration.Stage#getTestResults
     */
    Map getResults(String repoId, String type) {
        log.debug("Collecting JUnit XML Reports ('${type}') for ${repoId}")

        def testReportsPath = "${PipelineUtil.XUNIT_DOCUMENTS_BASE_DIR}/${repoId}/${type}"
        def testReportFiles = jUnitTestReport.loadTestReportsFromPath( "${steps.env.WORKSPACE}/${testReportsPath}")

        return [
            testReportFiles: testReportFiles,
            testResults: jUnitTestReport.parseTestReportFiles(testReportFiles),
        ]
    }
}
