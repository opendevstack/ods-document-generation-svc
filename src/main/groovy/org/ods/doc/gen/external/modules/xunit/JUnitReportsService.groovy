package org.ods.doc.gen.external.modules.xunit

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.project.data.TestType
import org.springframework.stereotype.Service

import javax.inject.Inject

@SuppressWarnings(['JavaIoPackageAccess', 'EmptyCatchBlock'])
@Slf4j
@Service
class JUnitReportsService {

    private final NexusService nexusService

    @Inject
    JUnitReportsService(NexusService nexusService) {
        this.nexusService = nexusService
    }
    
    Map combineTestResults(List<Map> testResults) {
        def result = [ testsuites: [] ]
        for (def i = 0; i < testResults.size(); i++) {
            result.testsuites.addAll(testResults[i].testsuites)
        }
        return result
    }
    
    int getNumberOfTestCases(Map testResults) {
        def result = 0

        testResults.testsuites.each { testsuite ->
            result += testsuite.testcases.size()
        }

        return result
    }
    
    List<File> loadTestReportsFromPath(String path, String typeIn = 'unit', String component = null) {
        def result = []
        try {
            def pattern = (StringUtils.isEmpty(component)) ? ~/.*${typeIn}.*\.xml$/ : ~/.*${component}.*\.xml$/
            new File(path).traverse(nameFilter: pattern, type: groovy.io.FileType.FILES) { file ->
                result << file
            }
        } catch (FileNotFoundException e) {}

        return result
    }

    Map parseTestReportFiles(List<File> files) {
        List<Map> testResults = []
        for (def i = 0; i < files.size(); i++) {
            testResults.add(org.ods.doc.gen.external.modules.xunit.parser.JUnitParser.parseJUnitXML(files[i].text))
        }
        return this.combineTestResults(testResults)
    }

    void downloadTestsResults(Map<String, String> testResultsURLs, String targetFolder) {
        testResultsURLs.each {
            nexusService.downloadAndExtractZip(it.value, targetFolder)
        }
    }

    Map getTestData(String tmpFolder, Map testResultsURLs, Map data) {
        downloadTestsResults(testResultsURLs as Map, tmpFolder)

        Map tests = createTestDataStructure()
        tests.each {
            Map testResult = getTestResults(it.key.capitalize(), tmpFolder, data.repo?.id?.capitalize())
            it.value.testReportFiles = testResult.testReportFiles
            it.value.testResults = testResult.testResults
        }
        return tests
    }

    private Map createTestDataStructure() {
        Map testData = [
                (TestType.UNIT.uncapitalize())        : [:],
                (TestType.ACCEPTANCE.uncapitalize())  : [:],
                (TestType.INSTALLATION.uncapitalize()): [:],
                (TestType.INTEGRATION.uncapitalize()) : [:],
        ]
        testData.each {
            it.value.testReportFiles = []
            it.value.testResults = [ testsuites: [] ]
        }
        return testData
    }

    private Map getTestResults(String typeIn = 'unit', String targetFolder, String component = null) {

        def testReportFiles = loadTestReportsFromPath(targetFolder, typeIn, component)

        return [
                // Load JUnit test report files from path
                testReportFiles: testReportFiles,
                // Parse JUnit test report files into a report
                testResults: parseTestReportFiles(testReportFiles),
        ]
    }

}
