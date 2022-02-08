package org.ods.doc.gen.external.modules.jira

import org.ods.doc.gen.core.test.SpecHelper
import org.ods.doc.gen.project.data.Project
import spock.lang.Ignore

import static org.ods.doc.gen.core.test.fixture.FixtureHelper.*

@Ignore
class JiraUseCaseSpec extends SpecHelper {

    JiraService jira
    Project project
    def steps = [:]
    JiraUseCase usecase

    def setup() {
        project = Spy(createProject())
        jira = Mock(JiraService) {
            createIssueTypeBug(_, _, _) >> {
                [
                    key   : "BUG-3",
                    fields: [
                        summary: "bug summary / name"
                    ]
                ]
            }
        }
        usecase = Spy(new JiraUseCase(project, steps, util, jira, logger))
    }

    def "apply test results as test issue labels"() {
        given:
        def testIssues = createJiraTestIssues()
        def testResults = createTestResults()

        when:
        usecase.applyXunitTestResultsAsTestIssueLabels(testIssues, testResults)

        then:
        1 * jira.removeLabelsFromIssue("JIRA-1", { return it == TestIssueLabels.values()*.toString() })
        1 * jira.addLabelsToIssue("JIRA-1", ["Succeeded"])
        0 * jira.addLabelsToIssue("JIRA-1", _)

        then:
        1 * jira.removeLabelsFromIssue("JIRA-2", { it == TestIssueLabels.values()*.toString() })
        1 * jira.addLabelsToIssue("JIRA-2", ["Error"])
        0 * jira.addLabelsToIssue("JIRA-2", _)

        then:
        1 * jira.removeLabelsFromIssue("JIRA-3", { it == TestIssueLabels.values()*.toString() })
        1 * jira.addLabelsToIssue("JIRA-3", ["Failed"])
        0 * jira.addLabelsToIssue("JIRA-3", _)

        then:
        1 * jira.removeLabelsFromIssue("JIRA-4", { it == TestIssueLabels.values()*.toString() })
        1 * jira.addLabelsToIssue("JIRA-4", ["Skipped"])
        0 * jira.addLabelsToIssue("JIRA-4", _)

        then:
        1 * jira.removeLabelsFromIssue("JIRA-5", { it == TestIssueLabels.values()*.toString() })
        1 * jira.addLabelsToIssue("JIRA-5", ["Missing"])
        0 * jira.addLabelsToIssue("JIRA-5", _)
    }

    def "check Jira issue matches test case"() {
        when:
        def issue = [key: "JIRA-123"]
        def testcase = [name: "JIRA123 test"]

        then:
        usecase.checkTestsIssueMatchesTestCase(issue, testcase)

        when:
        issue = [key: "JIRA-123"]
        testcase.name = "JIRA123-test"

        then:
        usecase.checkTestsIssueMatchesTestCase(issue, testcase)

        when:
        issue = [key: "JIRA-123"]
        testcase.name = "JIRA123_test"

        then:
        usecase.checkTestsIssueMatchesTestCase(issue, testcase)

        when:
        issue = [key: "JIRA-123"]
        testcase.name = "JIRA123test"

        then:
        !usecase.checkTestsIssueMatchesTestCase(issue, testcase)

        when:
        issue = [key: "JIRA-123"]
        testcase.name = "JIRA-123_test"

        then:
        !usecase.checkTestsIssueMatchesTestCase(issue, testcase)

        when:
        issue = [key: "JIRA-123"]
        testcase.name = "123_test"

        then:
        !usecase.checkTestsIssueMatchesTestCase(issue, testcase)
    }

    def "create bugs and block impacted test cases"() {
        given:
        // Test Parameters
        def testIssues = createSockShopJiraTestIssues()
        def failures = createSockShopTestResultFailures()
        def comment = "myComment"

        // Stubbed Method Responses
        def bug = [
            key   : "BUG-1",
            fields: [
                summary: "my-bug"
            ]
        ]

        when:
        usecase.createBugsForFailedTestIssues(testIssues, failures, comment)

        then:
        1 * project.getVersionFromReleaseStatusIssue() >> "1.0"

        then:
        1 * jira.createIssueTypeBug(project.jiraProjectKey, failures.first().type, failures.first().text, "1.0") >> bug

        then:
        1 * jira.createIssueLinkTypeBlocks(bug, {
            // the Jira issue that shall be linked to the bug
            it.key == "NET-140"
        })

        then:
        1 * jira.appendCommentToIssue(bug.key, comment)

        then:
        // verify that bug gets created and registered on the correct test issue
        project.data.jira.bugs.containsKey("BUG-1")
        project.data.jiraResolved.bugs.containsKey("BUG-1")
        testIssues.find { it.key == "NET-140" }.bugs.contains("BUG-1")
    }

    def "get document chapter data"() {
        given:
        // Test Parameters

        def docChapterFields = [
            (CustomIssueFields.HEADING_NUMBER): [id:"0"],
            (CustomIssueFields.CONTENT)       : [id: "1"],
        ]

        // Argument Constraints
        def jqlQuery = [
            fields: ['key', 'status', 'summary', 'labels', 'issuelinks',
                     docChapterFields[CustomIssueFields.CONTENT].id,
                     docChapterFields[CustomIssueFields.HEADING_NUMBER].id],
            jql: "project = ${project.jiraProjectKey} AND issuetype = '${IssueTypes.DOCUMENTATION_CHAPTER}'",
            expand: ['renderedFields'],
        ]

        // Stubbed Method Responses
        def jiraIssue1 = createJiraIssue("1", null, null, null, "DONE")
        jiraIssue1.fields["0"] = "1.0"
        jiraIssue1.fields.labels = [LabelPrefix.DOCUMENT+ "CSD"]
        jiraIssue1.renderedFields = [:]
        jiraIssue1.renderedFields["1"] = "<html>myContent1</html>"
        jiraIssue1.renderedFields.description = "<html>1-description</html>"

        def jiraIssue2 = createJiraIssue("2", null, null, null, "DONE")
        jiraIssue2.fields["0"] = "2.0"
        jiraIssue2.fields.labels = [LabelPrefix.DOCUMENT+ "SSDS"]
        jiraIssue2.renderedFields = [:]
        jiraIssue2.renderedFields["1"] = "<html>myContent2</html>"
        jiraIssue2.renderedFields.description = "<html>2-description</html>"

        def jiraIssue3 = createJiraIssue("3", null, null, null, "DONE")
        jiraIssue3.fields["0"] = "3.0"
        jiraIssue3.fields.labels = [LabelPrefix.DOCUMENT+ "DTP"]
        jiraIssue3.renderedFields = [:]
        jiraIssue3.renderedFields["1"] = "<html>myContent3</html>"
        jiraIssue3.renderedFields.description = "<html>3-description</html>"

        def jiraResult = [
            issues: [jiraIssue1, jiraIssue2, jiraIssue3],
        ]

        when:
        def result = usecase.getDocumentChapterData(project.jiraProjectKey)

        then:
        1 * project.getJiraFieldsForIssueType(IssueTypes.DOCUMENTATION_CHAPTER) >> docChapterFields
        1 * jira.searchByJQLQuery(jqlQuery) >> jiraResult

        then:
        def expected = [
            'JIRA-1': [
                section: 'sec1s0',
                number : '1.0',
                heading: '1-summary',
                documents: ['CSD'],
                content: '<html>myContent1</html>',
                status: 'DONE',
                key: 'JIRA-1',
                predecessors: [],
                versions: [],
            ],
            'JIRA-2': [
                section: 'sec2s0',
                number : '2.0',
                heading: '2-summary',
                documents: ['SSDS'],
                content: '<html>myContent2</html>',
                status: 'DONE',
                key: 'JIRA-2',
                predecessors: [],
                versions: [],
            ],
            'JIRA-3': [
                section: 'sec3s0',
                number : '3.0',
                heading: '3-summary',
                documents: ['DTP'],
                content: '<html>myContent3</html>',
                status: 'DONE',
                key: 'JIRA-3',
                predecessors: [],
                versions: [],
            ]
        ]

        result['JIRA-1'] == expected['JIRA-1']
        result['JIRA-2'] == expected['JIRA-2']
        result['JIRA-3'] == expected['JIRA-3']
    }

    def "get document chapter data with version"() {
        given:
        // Test Parameters
        def version = "myVersion"
        def predecessorKey = "PRED-1"

        def docChapterFields = [
            (CustomIssueFields.HEADING_NUMBER): [id:"0"],
            (CustomIssueFields.CONTENT)       : [id: "1"],
        ]

        // Argument Constraints
        def jqlQuery = [
            fields: ['key', 'status', 'summary', 'labels', 'issuelinks',
                     docChapterFields[CustomIssueFields.CONTENT].id,
                     docChapterFields[CustomIssueFields.HEADING_NUMBER].id],
            jql: "project = ${project.jiraProjectKey} AND issuetype = '${IssueTypes.DOCUMENTATION_CHAPTER}'" +
                " AND fixVersion = '${version}'",
            expand: ['renderedFields'],
        ]

        // Stubbed Method Responses
        def jiraIssue1 = createJiraIssue("1", null, null, null, "DONE")
        jiraIssue1.fields["0"] = "1.0"
        jiraIssue1.fields.labels = [LabelPrefix.DOCUMENT+ "CSD"]
        jiraIssue1.renderedFields = [:]
        jiraIssue1.renderedFields["1"] = "<html>myContent1</html>"
        jiraIssue1.renderedFields.description = "<html>1-description</html>"

        def jiraIssue2 = createJiraIssue("2", null, null, null, "DONE")
        jiraIssue2.fields["0"] = "2.0"
        jiraIssue2.fields.labels = [LabelPrefix.DOCUMENT+ "SSDS"]
        jiraIssue2.fields.issuelinks = [
            [type:[name: "Succeeds"], outwardIssue: [key: predecessorKey]],
            [type:[name: "Succeeds"], inwardIssue: [key: "Should not appwar the successor"]],
        ]
        jiraIssue2.renderedFields = [:]
        jiraIssue2.renderedFields["1"] = "<html>myContent2</html>"
        jiraIssue2.renderedFields.description = "<html>2-description</html>"

        def jiraIssue3 = createJiraIssue("3", null, null, null, "DONE")
        jiraIssue3.fields["0"] = "3.0"
        jiraIssue3.fields.labels = [LabelPrefix.DOCUMENT+ "DTP"]
        jiraIssue3.fields.issuelinks = [
            [type:[name: "AnotherLink"], outwardIssue: [key: "Should not appear other links outward"]],
            [type:[name: "AnotherLink2"], inwardIssue: [key: "Should not appear other links inward"]],
        ]
        jiraIssue3.renderedFields = [:]
        jiraIssue3.renderedFields["1"] = "<html>myContent3</html>"
        jiraIssue3.renderedFields.description = "<html>3-description</html>"

        def jiraResult = [
            issues: [jiraIssue1, jiraIssue2, jiraIssue3],
        ]

        when:
        def result = usecase.getDocumentChapterData(project.jiraProjectKey, version)

        then:
        1 * project.getJiraFieldsForIssueType(IssueTypes.DOCUMENTATION_CHAPTER) >> docChapterFields
        1 * jira.searchByJQLQuery(jqlQuery) >> jiraResult

        then:
        def expected = [
            'JIRA-1': [
                section: 'sec1s0',
                number : '1.0',
                heading: '1-summary',
                documents: ['CSD'],
                content: '<html>myContent1</html>',
                status: 'DONE',
                key: 'JIRA-1',
                predecessors: [],
                versions: [version],
            ],
            'JIRA-2': [
                section: 'sec2s0',
                number : '2.0',
                heading: '2-summary',
                documents: ['SSDS'],
                content: '<html>myContent2</html>',
                status: 'DONE',
                key: 'JIRA-2',
                predecessors: [predecessorKey],
                versions: [version],
            ],
            'JIRA-3': [
                section: 'sec3s0',
                number : '3.0',
                heading: '3-summary',
                documents: ['DTP'],
                content: '<html>myContent3</html>',
                status: 'DONE',
                key: 'JIRA-3',
                predecessors: [],
                versions: [version],
            ]
        ]

        result['JIRA-1'] == expected['JIRA-1']
        result['JIRA-2'] == expected['JIRA-2']
        result['JIRA-3'] == expected['JIRA-3']
    }

    def "get document chapter data with version and content empty"() {
        given:
        // Test Parameters
        def version = "myVersion"
        def predecessorKey = "PRED-1"

        def docChapterFields = [
            (CustomIssueFields.HEADING_NUMBER): [id:"0"],
            (CustomIssueFields.CONTENT)       : [id: "1"],
        ]

        // Argument Constraints
        def jqlQuery = [
            fields: ['key', 'status', 'summary', 'labels', 'issuelinks',
                     docChapterFields[CustomIssueFields.CONTENT].id,
                     docChapterFields[CustomIssueFields.HEADING_NUMBER].id],
            jql: "project = ${project.jiraProjectKey} AND issuetype = '${IssueTypes.DOCUMENTATION_CHAPTER}'" +
                " AND fixVersion = '${version}'",
            expand: ['renderedFields'],
        ]

        // Stubbed Method Responses
        def jiraIssue1 = createJiraIssue("1", null, null, null, "DONE")
        jiraIssue1.fields["0"] = "1.0"
        jiraIssue1.fields.labels = [LabelPrefix.DOCUMENT+ "CSD"]
        jiraIssue1.renderedFields = [:]
        jiraIssue1.renderedFields["1"] = "<html>myContent1</html>"
        jiraIssue1.renderedFields.description = "<html>1-description</html>"

        def jiraIssue2 = createJiraIssue("2", null, null, null, "DONE")
        jiraIssue2.fields["0"] = "2.0"
        jiraIssue2.fields.labels = [LabelPrefix.DOCUMENT+ "SSDS"]
        jiraIssue2.fields.issuelinks = [
            [type:[name: "Succeeds"], outwardIssue: [key: predecessorKey]],
            [type:[name: "Succeeds"], inwardIssue: [key: "Should not appwar the successor"]],
        ]
        jiraIssue2.renderedFields = [:]
        jiraIssue2.renderedFields["1"] = "<html>myContent2</html>"
        jiraIssue2.renderedFields.description = "<html>2-description</html>"

        def jiraIssue3 = createJiraIssue("3", null, null, null, "DONE")
        jiraIssue3.fields["0"] = "3.0"
        jiraIssue3.fields.labels = [LabelPrefix.DOCUMENT+ "DTP"]
        jiraIssue3.fields.issuelinks = [
            [type:[name: "AnotherLink"], outwardIssue: [key: "Should not appear other links outward"]],
            [type:[name: "AnotherLink2"], inwardIssue: [key: "Should not appear other links inward"]],
        ]
        jiraIssue3.renderedFields = [:]
        jiraIssue3.renderedFields["1"] = "" //Empty content
        jiraIssue3.renderedFields.description = "<html>3-description</html>"

        def jiraResult = [
            issues: [jiraIssue1, jiraIssue2, jiraIssue3],
        ]

        when:
        def result = usecase.getDocumentChapterData(project.jiraProjectKey, version)

        then:
        1 * project.getJiraFieldsForIssueType(IssueTypes.DOCUMENTATION_CHAPTER) >> docChapterFields
        1 * jira.searchByJQLQuery(jqlQuery) >> jiraResult

        then:
        def expected = [
            'JIRA-1': [
                section: 'sec1s0',
                number : '1.0',
                heading: '1-summary',
                documents: ['CSD'],
                content: '<html>myContent1</html>',
                status: 'DONE',
                key: 'JIRA-1',
                predecessors: [],
                versions: [version],
            ],
            'JIRA-2': [
                section: 'sec2s0',
                number : '2.0',
                heading: '2-summary',
                documents: ['SSDS'],
                content: '<html>myContent2</html>',
                status: 'DONE',
                key: 'JIRA-2',
                predecessors: [predecessorKey],
                versions: [version],
            ],
            'JIRA-3': [
                section: 'sec3s0',
                number : '3.0',
                heading: '3-summary',
                documents: ['DTP'],
                content: ' ', // With empty content a blank must be returned to avoid problems with preceding
                status: 'DONE',
                key: 'JIRA-3',
                predecessors: [],
                versions: [version],
            ]
        ]

        result['JIRA-1'] == expected['JIRA-1']
        result['JIRA-2'] == expected['JIRA-2']
        result['JIRA-3'] == expected['JIRA-3']
    }

    def "get document chapter data with multiple doclabels"() {
        given:
        // Test Parameters
        def version = "myVersion"
        def predecessorKey = "PRED-1"

        def docChapterFields = [
            (CustomIssueFields.HEADING_NUMBER): [id:"0"],
            (CustomIssueFields.CONTENT)       : [id: "1"],
        ]

        // Argument Constraints
        def jqlQuery = [
            fields: ['key', 'status', 'summary', 'labels', 'issuelinks',
                     docChapterFields[CustomIssueFields.CONTENT].id,
                     docChapterFields[CustomIssueFields.HEADING_NUMBER].id],
            jql: "project = ${project.jiraProjectKey} AND issuetype = '${IssueTypes.DOCUMENTATION_CHAPTER}'" +
                " AND fixVersion = '${version}'",
            expand: ['renderedFields'],
        ]

        // Stubbed Method Responses
        def jiraIssue1 = createJiraIssue("1", null, null, null, "DONE")
        jiraIssue1.fields["0"] = "1.0"
        jiraIssue1.fields.labels = [LabelPrefix.DOCUMENT+ "CSD", LabelPrefix.DOCUMENT+ "SSDS"]
        jiraIssue1.renderedFields = [:]
        jiraIssue1.renderedFields["1"] = "<html>myContent1</html>"
        jiraIssue1.renderedFields.description = "<html>1-description</html>"

        def jiraResult = [
            issues: [jiraIssue1],
        ]

        when:
        def result = usecase.getDocumentChapterData(project.jiraProjectKey, version)

        then:
        1 * project.getJiraFieldsForIssueType(IssueTypes.DOCUMENTATION_CHAPTER) >> docChapterFields
        1 * jira.searchByJQLQuery(jqlQuery) >> jiraResult

        then:
        def expected = [
            'JIRA-1': [
                section: 'sec1s0',
                number : '1.0',
                heading: '1-summary',
                documents: ['CSD', 'SSDS'],
                content: '<html>myContent1</html>',
                status: 'DONE',
                key: 'JIRA-1',
                predecessors: [],
                versions: [version],
            ]
        ]

        result == expected
    }

    def "match Jira test issues against test results"() {
        given:
        def testIssues = createJiraTestIssues()
        def testResults = createTestResults()

        def matched = [:]
        def matchedHandler = { result ->
            matched = result.collectEntries { jiraTestIssue, testcase ->
                [(jiraTestIssue.key.toString()), testcase.name]
            }
        }

        def mismatched = [:]
        def mismatchedHandler = { result ->
            mismatched = result.collect { it.key }
        }

        when:
        usecase.matchTestIssuesAgainstTestResults(testIssues, testResults, matchedHandler, mismatchedHandler)

        then:
        def expectedMatched = [
            "JIRA-1": "JIRA1_my-testcase-1",
            "JIRA-2": "JIRA2_my-testcase-2",
            "JIRA-3": "JIRA3_my-testcase-3",
            "JIRA-4": "JIRA4_my-testcase-4"
        ]

        def expectedMismatched = [
            "JIRA-5"
        ]

        matched == expectedMatched
        mismatched == expectedMismatched
    }

    def "match Jira test issues against test results having duplicate test results"() {
        given:
        def testIssues = createJiraTestIssues()
        def testResults = createTestResultsWithDuplicates()

        def matched = [:]
        def matchedHandler = { result ->
            matched = result.collectEntries { jiraTestIssue, testcase ->
                [(jiraTestIssue.key.toString()), testcase.name]
            }
        }

        def mismatched = [:]
        def mismatchedHandler = { result ->
            mismatched = result.collect { it.key }
        }

        when:
        usecase.matchTestIssuesAgainstTestResults(testIssues, testResults, matchedHandler, mismatchedHandler)

        then:
        def e = thrown(IllegalStateException)
        e.message == 'Error: found duplicated Jira tests. Check tests with key: [JIRA-1, JIRA-2]'
    }

    def "match Jira test issues against test results having duplicate test results with flag check duplicate to false"() {
        given:
        def testIssues = createJiraTestIssues()
        def testResults = createTestResultsWithDuplicates()

        def matched = [:]
        def matchedHandler = { result ->
            matched = result.collectEntries { jiraTestIssue, testcase ->
                [(jiraTestIssue.key.toString()), testcase.name]
            }
        }

        def mismatched = [:]
        def mismatchedHandler = { result ->
            mismatched = result.collect { it.key }
        }

        when:
        usecase.matchTestIssuesAgainstTestResults(testIssues, testResults, matchedHandler, mismatchedHandler, false)

        then:
        def expectedMatched = [
            "JIRA-1": "JIRA1_my-testcase-3",
            "JIRA-2": "JIRA2_my-testcase-4"
        ]

        def expectedMismatched = [
            "JIRA-3, JIRA-4, JIRA-5"
        ]

        matched == expectedMatched
        mismatched.toString() == expectedMismatched.toString()
    }

    def "report test results for component in DEV"() {
        given:
        project.buildParams.targetEnvironmentToken = "D"

        def support = Mock(JiraUseCaseSupport)
        usecase.setSupport(support)

        // Test Parameters
        def componentName = "myComponent"
        def testTypes = ["myTestType"]
        def testResults = createSockShopTestResults()

        // Stubbed Method Responses
        def testIssues = createSockShopJiraTestIssues()

        when:
        usecase.reportTestResultsForComponent(componentName, testTypes, testResults)

        then:
        1 * project.getAutomatedTests(componentName, testTypes) >> testIssues

        then:
        1 * support.applyXunitTestResults(testIssues, testResults)
        1 * util.warnBuildIfTestResultsContainFailure(testResults)
        1 * util.warnBuildAboutUnexecutedJiraTests(_)
    }

    def "report test results for component with unexecuted Jira tests"() {
        given:
        def support = Mock(JiraUseCaseSupport)
        usecase.setSupport(support)

        // Test Parameters
        def componentName = "myComponent"
        def testTypes = ["myTestType"]
        def testResults = [:] // unexecuted tests

        // Stubbed Method Responses
        def testIssues = createJiraTestIssues()

        when:
        usecase.reportTestResultsForComponent(componentName, testTypes, testResults)

        then:
        1 * project.getAutomatedTests(componentName, testTypes) >> testIssues

        then:
        1 * util.warnBuildAboutUnexecutedJiraTests(testIssues)
    }

    def "report test results for component in QA"() {
        given:
        project.buildParams.targetEnvironmentToken = "Q"

        def support = Mock(JiraUseCaseSupport)
        usecase.setSupport(support)

        // Test Parameters
        def componentName = "myComponent"
        def testTypes = ["myTestType"]
        def testResults = createSockShopTestResults()

        // Argument Constraints
        def errors = createSockShopTestResultErrors()
        def failures = createSockShopTestResultFailures()

        // Stubbed Method Responses
        def testIssues = createSockShopJiraTestIssues()

        when:
        usecase.reportTestResultsForComponent(componentName, testTypes, testResults)

        then:
        1 * project.getAutomatedTests(componentName, testTypes) >> testIssues

        then:
        1 * support.applyXunitTestResults(testIssues, testResults)
        1 * util.warnBuildIfTestResultsContainFailure(testResults)

        then:
        1 * usecase.createBugsForFailedTestIssues(testIssues, errors, steps.env.runDisplayUrl) >> null
        1 * usecase.createBugsForFailedTestIssues(testIssues, failures, steps.env.runDisplayUrl) >> null
    }

    def "report test results for component in PROD"() {
        given:
        project.buildParams.targetEnvironmentToken = "Q"

        def support = Mock(JiraUseCaseSupport)
        usecase.setSupport(support)

        // Test Parameters
        def componentName = "myComponent"
        def testTypes = ["myTestType"]
        def testResults = createSockShopTestResults()

        // Argument Constraints
        def errors = createSockShopTestResultErrors()
        def failures = createSockShopTestResultFailures()

        // Stubbed Method Responses
        def testIssues = createSockShopJiraTestIssues()

        when:
        usecase.reportTestResultsForComponent(componentName, testTypes, testResults)

        then:
        1 * project.getAutomatedTests(componentName, testTypes) >> testIssues

        then:
        1 * support.applyXunitTestResults(testIssues, testResults)
        1 * util.warnBuildIfTestResultsContainFailure(testResults)

        then:
        1 * usecase.createBugsForFailedTestIssues(testIssues, errors, steps.env.runDisplayUrl) >> null
        1 * usecase.createBugsForFailedTestIssues(testIssues, failures, steps.env.runDisplayUrl) >> null
    }

    def "update Jira release status build number"() {
        given:
        project.buildParams.releaseStatusJiraIssueKey = "JIRA-4711"
        project.buildParams.version = "1.0"
        steps.env.buildNumber = "0815"

        when:
        usecase.updateJiraReleaseStatusBuildNumber()

        then:
        1 * project.getJiraFieldsForIssueType(IssueTypes.RELEASE_STATUS) >> [
            "Release Build": [
                id: "customfield_2"
            ]
        ]

        then:
        1 * jira.updateTextFieldsOnIssue("JIRA-4711", [
            "customfield_2": "1.0-0815"
        ])
    }

    def "update Jira release status result"() {
        given:
        project.buildParams.releaseStatusJiraIssueKey = "JIRA-4711"
        project.buildParams.version = "1.0"
        steps.env.buildNumber = "0815"
        steps.env.runDisplayUrl = "http://jenkins"

        def error = new RuntimeException("Oh no!")

        when:
        usecase.updateJiraReleaseStatusResult(error.message, true)

        then:
        1 * project.getJiraFieldsForIssueType(IssueTypes.RELEASE_STATUS) >> [
            "Release Manager Status": [
                id: "customfield_1"
            ]
        ]

        then:
        1 * jira.updateSelectListFieldsOnIssue("JIRA-4711", [
            "customfield_1": "Failed"
        ])

        then:
        1 * jira.appendCommentToIssue("JIRA-4711", "${error.message}\n\nSee: ${steps.env.runDisplayUrl}")
    }

    def "update Jira release status result without error"() {
        given:
        project.buildParams.releaseStatusJiraIssueKey = "JIRA-4711"
        project.buildParams.version = "1.0"
        steps.env.buildNumber = "0815"

        when:
        usecase.updateJiraReleaseStatusResult("", false)

        then:
        1 * project.getJiraFieldsForIssueType(IssueTypes.RELEASE_STATUS) >> {
            return [
                "Release Manager Status": [
                    id: "customfield_1"
                ]
            ]
        }

        then:
        1 * jira.updateSelectListFieldsOnIssue("JIRA-4711", [
            "customfield_1": "Successful"
        ])
    }

    def "walk test issues and test results"() {
        given:
        def testIssues = createJiraTestIssues()
        def testResults = createTestResults()

        def result = [:]
        def visitor = { jiraTestIssue, testcase, isMatch ->
            if (isMatch) result[jiraTestIssue.key] = testcase.name
        }

        when:
        usecase.walkTestIssuesAndTestResults(testIssues, testResults, visitor)

        then:
        def expected = [
            "JIRA-1": "JIRA1_my-testcase-1",
            "JIRA-2": "JIRA2_my-testcase-2",
            "JIRA-3": "JIRA3_my-testcase-3",
            "JIRA-4": "JIRA4_my-testcase-4"
        ]

        result == expected
    }

    def "get version from Jira"() {
        given:
        def textFieldsOfIssue
        def jira = Mock(JiraService) {
            getTextFieldsOfIssue(*_) >> { issueIdOrKey, List <String> fields ->
                fields.collectEntries { [(it): textFieldsOfIssue] }
            }
        }
        def usecase = Spy(new JiraUseCase(project, steps, util, jira, logger))
        def result

        when:
        textFieldsOfIssue = null
        usecase.getVersionFromReleaseStatusIssue()

        then:
        project.buildParams.releaseStatusJiraIssueKey >> "KEY-1"
        project.getJiraFieldsForIssueType(IssueTypes.RELEASE_STATUS) >> [(CustomIssueFields.RELEASE_VERSION): [id: "field_0"]]
        def e = thrown(IllegalArgumentException)
        e.message.contains('Unable to obtain version name from release status issue')

        when:
        textFieldsOfIssue = [somethingElse: "something else"]
        usecase.getVersionFromReleaseStatusIssue()

        then:
        project.buildParams.releaseStatusJiraIssueKey >> "KEY-1"
        project.getJiraFieldsForIssueType(IssueTypes.RELEASE_STATUS) >> [(CustomIssueFields.RELEASE_VERSION): [id: "field_0"]]
        e = thrown(IllegalArgumentException)
        e.message.contains('Unable to obtain version name from release status issue')

        when:
        textFieldsOfIssue = [name: "versionX"]
        result = usecase.getVersionFromReleaseStatusIssue()

        then:
        project.buildParams.releaseStatusJiraIssueKey >> "KEY-1"
        project.getJiraFieldsForIssueType(IssueTypes.RELEASE_STATUS) >> [(CustomIssueFields.RELEASE_VERSION): [id: "field_0"]]
        result == "versionX"
    }

    def "get HTML Image As Base64"() {
        given:
        def jiraUrl = new URI("https://jira.com")
        def contentType = "contentType"
        def binaryData = "binaryData".bytes
        def binaryDataCoded = binaryData.encodeBase64()

        def jira = Mock(JiraService) {
            getBaseURL() >> {
                jiraUrl
            }
            getFileFromJira(*_) >> {
                [ contentType: contentType, data: binaryData ]
            }
        }
        def usecase = Spy(new JiraUseCase(project, steps, util, jira, logger))

        when: 'we have a simple image tag'
        def result = usecase.convertHTMLImageSrcIntoBase64Data("<img src=\"${jiraUrl}/something.png\">")

        then:
        result == "<img src=\"data:${contentType};base64,${binaryDataCoded}\">"


        when: 'we a complex image tag structure with two extensions'
        result = usecase.convertHTMLImageSrcIntoBase64Data("<img src=\"${jiraUrl}/something.png\" imagetext=\"something.png\">")

        then:
        result == "<img src=\"data:${contentType};base64,${binaryDataCoded}\" imagetext=\"something.png\">"

        when: 'we have two images'
        result = usecase.convertHTMLImageSrcIntoBase64Data("<img src=\"${jiraUrl}/something.png\" imagetext=\"something.png\">aaa<img src=\"${jiraUrl}/something2.png\" imagetext=\"something2.png\">")

        then:
        result == "<img src=\"data:${contentType};base64,${binaryDataCoded}\" imagetext=\"something.png\">aaa<img src=\"data:${contentType};base64,${binaryDataCoded}\" imagetext=\"something2.png\">"
    }
}
