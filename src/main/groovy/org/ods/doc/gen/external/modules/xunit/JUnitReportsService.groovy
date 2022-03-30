package org.ods.doc.gen.external.modules.xunit

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.project.data.TestType
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson

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

    Map getTestData(Map data) {
        String tmpFolder = data.tmpFolder as String
        Map testResultsURLs = data.build.testResultsURLs as Map
        String component = data.repo?.id?.capitalize()

        Map<String, Map> tests = downloadTestsResults(testResultsURLs as Map, tmpFolder, component)

        tests.each {
            String targetFolder = it.value.targetFolder as String
            Map testResult = getTestResults(it.key.capitalize(), targetFolder, component)
            it.value.testReportFiles = testResult.testReportFiles
            it.value.testResults = testResult.testResults
        }
        log.debug("Result from obtaining tests data: " )
        log.debug(prettyPrint(toJson(tests)))
        return tests
    }

    private Map<String, Map> downloadTestsResults(Map<String, String> testResultsURLs, String targetFolder, String component) {
        Map<String, Map> testsResults = createTestDataStructure(component)
        String unitKeyTests = TestType.UNIT.toLowerCase()

        testResultsURLs.each {testResultUrl ->
            testsResults.each {testResult ->
                String testResultUrlKey = testResultUrl.key.toLowerCase()
                String testResultKey = testResult.key.toLowerCase()
                if (unitKeyTests == testResultKey) {
                    if ((testResultUrlKey.contains(unitKeyTests)) && component && (testResultUrlKey.contains(component.toLowerCase()))) {
                        testResult.value.targetFolder = downloadAndExtractZip(testResultUrl.value, targetFolder, testResultKey)
                    }
                } else {
                    if (testResultUrlKey == testResultKey) {
                        testResult.value.targetFolder = downloadAndExtractZip(testResultUrl.value, targetFolder, testResultKey)
                    }
                }
            }
        }

        return testsResults
    }

    private String downloadAndExtractZip(String urlToDownload, String targetFolder, String subFolder) {
        Path targetFolderWithKey = Paths.get(targetFolder, subFolder)
        Files.createDirectories(targetFolderWithKey)
        nexusService.downloadAndExtractZip(urlToDownload, targetFolderWithKey.toString())
        return targetFolderWithKey.toString()
    }

    private Map<String, Map> createTestDataStructure(String component = null) {
        Map testData = [ : ]
        if (component) {
            testData.put(TestType.UNIT.uncapitalize(), [:])
        } else {
            testData.putAll([
                    (TestType.ACCEPTANCE.uncapitalize())  : [:],
                    (TestType.INSTALLATION.uncapitalize()): [:],
                    (TestType.INTEGRATION.uncapitalize()) : [:],
            ])
        }

        testData.each {
            it.value.testReportFiles = []
            it.value.testResults = [ testsuites: [] ]
        }
        return testData
    }

    private Map getTestResults(String typeIn = 'unit', String targetFolder, String component = null) {
        if (targetFolder == null) {
            return [
                    testReportFiles: [],
                    testResults: [:],
            ]
        }
        def testReportFiles = loadTestReportsFromPath(targetFolder, typeIn, component)

        return [
                // Load JUnit test report files from path
                testReportFiles: testReportFiles,
                // Parse JUnit test report files into a report
                testResults: parseTestReportFiles(testReportFiles),
        ]
    }

}
