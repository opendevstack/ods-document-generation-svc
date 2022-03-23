package org.ods.doc.gen.external.modules.xunit

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.external.modules.xunit.parser.JUnitParser
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

import static org.ods.doc.gen.core.test.fixture.FixtureHelper.createJUnitXMLTestResults

class JUnitReportsServiceSpec extends Specification {

    NexusService nexusService
    JUnitReportsService service

    @Rule
    TemporaryFolder temporaryFolder

    def setup() {
        nexusService = Mock(NexusService)
        service = new JUnitReportsService(nexusService)
    }

    def "combine test results"() {
        given:
        def testResult1 = [
            testsuites: [
                [
                    testcases: [
                        [ a: 1 ]
                    ]
                ]
            ]
        ]

        def testResult2 = [
            testsuites: [
                [
                    testcases: [
                        [ b: 2 ]
                    ]
                ]
            ]
        ]

        def testResult3 = [
            testsuites: [
                [
                    testcases: [
                        [ c: 3 ]
                    ]
                ]
            ]
        ]

        when:
        def result = service.combineTestResults([testResult1, testResult2, testResult3 ])

        then:
        result == [
            testsuites: [
                [
                    testcases: [
                        [ a: 1 ]
                    ]
                ],
                [
                    testcases: [
                        [ b: 2 ]
                    ]
                ],
                [
                    testcases: [
                        [ c: 3 ]
                    ]
                ]
            ]
        ]
    }

    def "get number of test cases"() {
        given:
        def testResults = [
            testsuites: [
                [
                    testcases: [
                        [ a: 1 ], [ b: 2 ], [ c: 3 ]
                    ]
                ]
            ]
        ]

        when:
        def result = service.getNumberOfTestCases(testResults)

        then:
        result == 3
    }

    def "load test reports from path"() {
        given:
        def xmlFiles = Files.createTempDirectory("junit-test-reports-")
        def xmlFile1 = Files.createTempFile(xmlFiles, "junit", ".xml") << "JUnit XML Report 1"
        def xmlFile2 = Files.createTempFile(xmlFiles, "junit", ".xml") << "JUnit XML Report 2"

        when:
        def result = service.loadTestReportsFromPath(xmlFiles.toString())

        then:
        result.size() == 2
        result.collect { it.text }.sort() == ["JUnit XML Report 1", "JUnit XML Report 2"]

        cleanup:
        xmlFiles.toFile().deleteDir()
    }

    def "load test reports from path with empty path"() {
        given:
        def xmlFiles = Files.createTempDirectory("junit-test-reports-")

        when:
        def result = service.loadTestReportsFromPath(xmlFiles.toString())

        then:
        result.isEmpty()

        cleanup:
        xmlFiles.toFile().deleteDir()
    }

    def "parse test report files"() {
        given:
        def xmlFiles = Files.createTempDirectory("junit-test-reports-")
        def xmlFile = Files.createTempFile(xmlFiles, "junit", ".xml").toFile()
        xmlFile << "<?xml version='1.0' ?>\n" + createJUnitXMLTestResults()

        when:
        def result = service.parseTestReportFiles([xmlFile])

        then:
        def expected = [
            testsuites: JUnitParser.parseJUnitXML(xmlFile.text).testsuites
        ]

        result == expected

        cleanup:
        xmlFiles.deleteDir()
    }

    def "download and unzip tests files"() {
        given:
        Path temporaryFolder = Files.createTempDirectory("junit-test-reports-")
        String path = temporaryFolder.toFile().getAbsolutePath()
        Map<String, String> listOfFiles = [
                "Unit-releasemanager": "/repository/leva-documentation/ordgp/ordgp-releasemanager/666/unit-ordgp-ordgp-releasemanager.zip",
                "Acceptance" : "/repository/leva-documentation/ordgp/ordgp-releasemanager/666/acceptance-ordgp-ordgp-releasemanager.zip",
                'Installation' : "/repository/leva-documentation/ordgp/ordgp-releasemanager/666/installation-ordgp-ordgp-releasemanager.zip",
                'Integration' : "/repository/leva-documentation/ordgp/ordgp-releasemanager/666/integration-ordgp-ordgp-releasemanager.zip",
        ]
        String component = null
        when:
        service.downloadTestsResults(listOfFiles, path, component)

        then:
        0 * nexusService.downloadAndExtractZip(listOfFiles.Unit, "${path}/unit")
        1 * nexusService.downloadAndExtractZip(listOfFiles.Acceptance, "${path}/acceptance")
        1 * nexusService.downloadAndExtractZip(listOfFiles.Installation, "${path}/installation")
        1 * nexusService.downloadAndExtractZip(listOfFiles.Integration, "${path}/integration")

        cleanup:
        temporaryFolder.deleteDir()
    }

    def "download and unzip tests files for component"() {
        given:
        Path temporaryFolder = Files.createTempDirectory("junit-test-reports-")
        String path = temporaryFolder.toFile().getAbsolutePath()
        Map<String, String> listOfFiles = [
                "Unit-releasemanager": "/repository/leva-documentation/ordgp/ordgp-releasemanager/666/unit-ordgp-ordgp-releasemanager.zip",
                "Acceptance" : "/repository/leva-documentation/ordgp/ordgp-releasemanager/666/acceptance-ordgp-ordgp-releasemanager.zip",
                'Installation' : "/repository/leva-documentation/ordgp/ordgp-releasemanager/666/installation-ordgp-ordgp-releasemanager.zip",
                'Integration' : "/repository/leva-documentation/ordgp/ordgp-releasemanager/666/integration-ordgp-ordgp-releasemanager.zip",
        ]
        String component = "releasemanager"
        when:
        service.downloadTestsResults(listOfFiles, path, component)

        then:
        1 * nexusService.downloadAndExtractZip(listOfFiles."Unit-releasemanager", "${path}/unit")
        0 * nexusService.downloadAndExtractZip(listOfFiles.Acceptance, "${path}/acceptance")
        0 * nexusService.downloadAndExtractZip(listOfFiles.Installation, "${path}/installation")
        0 * nexusService.downloadAndExtractZip(listOfFiles.Integration, "${path}/integration")

        cleanup:
        temporaryFolder.deleteDir()
    }
}
