package org.ods.shared.lib.orchestration.usecase



import  org.ods.shared.lib.orchestration.parser.JUnitParser
import org.ods.shared.lib.util.IPipelineSteps
import  org.ods.shared.lib.orchestration.util.Project

@SuppressWarnings(['JavaIoPackageAccess', 'EmptyCatchBlock'])
class JUnitTestReportsUseCase {

    private final Project project
    private final IPipelineSteps steps

    JUnitTestReportsUseCase(Project project, IPipelineSteps steps) {
        this.project = project
        this.steps = steps
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
            testResults.add(JUnitParser.parseJUnitXML(files[i].text))
        }
        return this.combineTestResults(testResults)
    }

    void reportTestReportsFromPathToJenkins(String path) {
        this.steps.junit("${path}/**/*.xml")
    }

}
