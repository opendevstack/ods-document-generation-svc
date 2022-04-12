package org.ods.doc.gen.external.modules.xunit

import groovy.util.logging.Slf4j
import org.ods.doc.gen.core.FileSystemHelper
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Service
class JUnitReportsService {

    private final NexusService nexusService
    private final FileSystemHelper fileSystemHelper

    @Inject
    JUnitReportsService(NexusService nexusService, FileSystemHelper fileSystemHelper) {
        this.fileSystemHelper = fileSystemHelper
        this.nexusService = nexusService
    }
    
    Map combineTestResults(List<Map> testResults) {
        List testSuites = []
        for (def i = 0; i < testResults.size(); i++) {
            List elementsToAdd = testResults[i].testsuites as List
            testSuites.addAll(elementsToAdd as List)
        }
        return [ testsuites: testSuites ]
    }
    
    int getNumberOfTestCases(Map testResults) {
        def result = 0

        testResults.testsuites.each { testsuite ->
            result += testsuite.testcases.size()
        }

        return result
    }

    Map parseTestReportFiles(List<File> files) {
        List<Map> testResults = []
        for (def i = 0; i < files.size(); i++) {
            testResults.add(org.ods.doc.gen.external.modules.xunit.parser.JUnitParser.parseJUnitXML(files[i].text))
        }
        return this.combineTestResults(testResults)
    }

    Map getTestData(Map data, List<String> testsTypes) {
        String tmpFolder = data.tmpFolder as String
        Map testResultsURLs = data.build.testResultsURLs as Map
        String component = data.repo?.id?.capitalize()

        Map<String, Map> tests = downloadTestsResults(testResultsURLs as Map, tmpFolder, testsTypes, component)

        tests.each {
            String targetFolder = it.value.targetFolder as String
            Map testResult = getTestResults(it.key.capitalize(), targetFolder, component)
            it.value.testReportFiles = testResult.testReportFiles
            it.value.testResults = testResult.testResults
        }
        return tests
    }

    private Map<String, Map> downloadTestsResults(Map<String, String> testResultsURLs,
                                                  String targetFolder,
                                                  List<String> testsTypes,
                                                  String component) {
        Map<String, Map> testsResults = createTestDataStructure(testsTypes, component)
        testsResults.each {testResult ->
            String testType = (component)? "${testResult.key}-${component.toLowerCase()}" : "${testResult.key}"
            String url = testResultsURLs[testType]
            if (url){
                testResult.value.targetFolder = downloadAndExtractZip(url, targetFolder, testType)
            } else {
                throw new RuntimeException("Could not download test results of type ${testType} from testResultsURLs[${testType}] = ${url} ")
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

    private Map<String, Map> createTestDataStructure(List<String> testsTypes, String component = null) {
        Map testData = [ : ]
        testsTypes.each {
            testData.put(it, [:])
        }

        testData.each {
            it.value.testReportFiles = []
            it.value.testResults = [ testsuites: [] ]
        }
        return testData
    }

    private Map getTestResults(String typeIn, String targetFolder, String component) {
        if (targetFolder == null) {
            return [
                    testReportFiles: [],
                    testResults: [:],
            ]
        }
        List<File> testReportFiles = fileSystemHelper.loadFilesFromPath(targetFolder, "xml")
        Map testResults = parseTestReportFiles(testReportFiles)
        return [
                testReportFiles: testReportFiles,
                testResults: testResults,
        ]
    }

}
