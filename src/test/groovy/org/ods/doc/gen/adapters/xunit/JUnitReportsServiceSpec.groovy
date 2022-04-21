package org.ods.doc.gen.adapters.xunit

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ods.doc.gen.core.FileSystemHelper
import org.ods.doc.gen.leva.doc.services.xunit.JUnitReportsService
import org.ods.doc.gen.leva.doc.services.xunit.parser.JUnitParser
import spock.lang.Specification

import java.nio.file.Files

import static org.ods.doc.gen.core.test.fixture.FixtureHelper.createJUnitXMLTestResults

class JUnitReportsServiceSpec extends Specification {

    org.ods.doc.gen.adapters.nexus.NexusService nexusService
    JUnitReportsService service

    @Rule
    TemporaryFolder temporaryFolder

    def setup() {
        nexusService = Mock(org.ods.doc.gen.adapters.nexus.NexusService)
        service = new JUnitReportsService(nexusService, new FileSystemHelper())
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

}
