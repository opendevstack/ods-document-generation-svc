package org.ods.doc.gen.external.modules.xunit

import groovy.util.logging.Slf4j
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.project.data.Project
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Paths

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
    
    List<File> loadTestReportsFromPath(String path) {
        def result = []
        try {
            new File(path).traverse(nameFilter: ~/.*\.xml$/, type: groovy.io.FileType.FILES) { file ->
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
            nexusService.downloadZip(it.value, targetFolder)
        }
    }

}
