package org.ods.shared.lib.jira

import groovy.util.logging.Slf4j
import  org.ods.doc.gen.leva.doc.services.MROPipelineUtil
import  org.ods.shared.lib.project.data.Project
import  org.ods.shared.lib.project.data.JiraDataItem
import org.ods.shared.lib.project.data.ProjectData
import org.ods.shared.lib.xunit.parser.JUnitParser
import org.springframework.stereotype.Service

import javax.inject.Inject

@SuppressWarnings(['IfStatementBraces', 'LineLength'])
@Slf4j
@Service
class JiraUseCase {

    private final Project project
    private final JiraService jira
    private final MROPipelineUtil util

    @Inject
    JiraUseCase(Project project, MROPipelineUtil util, JiraService jira) {
        this.project = project
        this.util = util
        this.jira = jira
    }

    boolean checkTestsIssueMatchesTestCase(Map testIssue, Map testCase) {
        def issueKeyClean = testIssue.key.replaceAll('-', '')
        return testCase.name.startsWith("${issueKeyClean} ") ||
            testCase.name.startsWith("${issueKeyClean}-") ||
            testCase.name.startsWith("${issueKeyClean}_")
    }

    String convertHTMLImageSrcIntoBase64Data(String html) {
        def server = this.jira.baseURL

        def pattern = ~/src="(${server}.*?\.(?:gif|GIF|jpg|JPG|jpeg|JPEG|png|PNG))"/
        def result = html.replaceAll(pattern) { match ->
            def src = match[1]
            def img = this.jira.getFileFromJira(src)
            return "src=\"data:${img.contentType};base64,${img.data.encodeBase64()}\""
        }

        return result
    }

    void createBugsForFailedTestIssues(ProjectData projectData, List testIssues, Set testFailures, String comment) {
        if (!this.jira) return

        testFailures.each { failure ->
            // FIXME: this.project.versionFromReleaseStatusIssue loads data from Jira and should therefore be called not more
            // than once. However, it's also called via this.getVersionFromReleaseStatusIssue in Project.groovy.
            String version = projectData.versionFromReleaseStatusIssue
            def bug = this.jira.createIssueTypeBug(
                projectData.jiraProjectKey, failure.type, failure.text, version)

            // Maintain a list of all Jira test issues affected by the current bug
            def bugAffectedTestIssues = [:]
            this.walkTestIssuesAndTestResults(testIssues, failure) { testIssue, testCase, isMatch ->
                // Find the testcases within the current failure that corresponds to a Jira test issue
                if (isMatch) {
                    // Add a reference to the current bug to the Jira test issue
                    if (null == testIssue.bugs) {
                        testIssue.bugs = []
                    }
                    testIssue.bugs << bug.key

                    // Add a link to the current bug on the Jira test issue (within Jira)
                    this.jira.createIssueLinkTypeBlocks(bug, testIssue)

                    bugAffectedTestIssues << [(testIssue.key): testIssue]
                }
            }

            // Create a JiraDataItem from the newly created bug
            def bugJiraDataItem = new JiraDataItem(project, [ // add project reference for access to Project.JiraDataItem
              key: bug.key,
              name: failure.type,
              assignee: "Unassigned",
              dueDate: "",
              status: "TO DO",
              tests: bugAffectedTestIssues.keySet() as List,
              versions: [ "${version}" ]
            ], Project.JiraDataItem.TYPE_BUGS)

            // Add JiraDataItem into the Jira data structure
            projectData.data.jira.bugs[bug.key] = bugJiraDataItem

            // Add the resolved JiraDataItem into the Jira data structure
            projectData.data.jiraResolved.bugs[bug.key] = bugJiraDataItem.cloneIt()
            projectData.data.jiraResolved.bugs[bug.key].tests = bugAffectedTestIssues.values() as List

            this.jira.appendCommentToIssue(bug.key, comment)
        }
    }

    void matchTestIssuesAgainstTestResults(List testIssues, Map testResults,
                                           Closure matchedHandler, Closure unmatchedHandler = null,
                                           boolean checkDuplicateTestResults = true) {
        def duplicateKeysErrorMessage = "Error: found duplicated Jira tests. Check tests with key: "
        def duplicatesKeys = []

        def result = [
            matched: [:],
            unmatched: []
        ]

        this.walkTestIssuesAndTestResults(testIssues, testResults) { testIssue, testCase, isMatch ->
            if (isMatch) {
                if (result.matched.get(testIssue) != null) {
                    duplicatesKeys.add(testIssue.key)
                }

                result.matched << [
                    (testIssue): testCase
                ]
            }
        }

        testIssues.each { testIssue ->
            if (!result.matched.keySet().contains(testIssue)) {
                result.unmatched << testIssue
            }
        }

        if (matchedHandler) {
            matchedHandler(result.matched)
        }

        if (unmatchedHandler) {
            unmatchedHandler(result.unmatched)
        }

        if (checkDuplicateTestResults && duplicatesKeys) {
            throw new IllegalStateException(duplicateKeysErrorMessage + duplicatesKeys);
        }
    }

    void reportTestResultsForComponent(ProjectData projectData, String componentName, List<String> testTypes, Map testResults) {
        if (!this.jira) return

        def testComponent = "${componentName ?: 'project'}"
        def testMessage = componentName ? " for component '${componentName}'" : ''
        if (log.debugMode) {
            log.debug('Reporting unit test results to corresponding test cases in Jira' +
                "${testMessage}. Test type: '${testTypes}'.\nTest results: ${testResults}")
        }

        log.startClocked("${testComponent}-jira-fetch-tests-${testTypes}")
        def testIssues = projectData.getAutomatedTests(componentName, testTypes)
        log.debugClocked("${testComponent}-jira-fetch-tests-${testTypes}",
            "Found automated tests$testMessage. Test type: ${testTypes}: " +
                "${testIssues?.size()}")

        this.util.warnBuildIfTestResultsContainFailure(testResults)
        this.matchTestIssuesAgainstTestResults(testIssues, testResults, null) { unexecutedJiraTests ->
            if (!unexecutedJiraTests.isEmpty()) {
                this.util.warnBuildAboutUnexecutedJiraTests(unexecutedJiraTests)
            }
        }

        log.info("${testComponent}-jira-report-tests-${testTypes}")
        applyXunitTestResultsAsTestIssueLabels(testIssues, testResults)
        log.debug("${testComponent}-jira-report-tests-${testTypes}")
        if (['Q', 'P'].contains(projectData.buildParams.targetEnvironmentToken)) {
            log.info("${testComponent}-jira-report-bugs-${testTypes}")
            // Create bugs for erroneous test issues
            def errors = JUnitParser.Helper.getErrors(testResults)
            this.createBugsForFailedTestIssues(projectData, testIssues, errors, projectData.data.env.RUN_DISPLAY_URL)

            // Create bugs for failed test issues
            def failures = JUnitParser.Helper.getFailures(testResults)
            this.createBugsForFailedTestIssues(projectData, testIssues, failures, projectData.data.env.RUN_DISPLAY_URL)
            log.debug("${testComponent}-jira-report-bugs-${testTypes}")
        }
    }

    Long getLatestDocVersionId(ProjectData projectData, List < Map > trackingIssues) {
        def documentationTrackingIssueFields = projectData.getJiraFieldsForIssueType(IssueTypes.DOCUMENTATION_TRACKING)
        def documentVersionField = documentationTrackingIssueFields[CustomIssueFields.DOCUMENT_VERSION].id as String

        // We will use the biggest ID available
        def versionList = trackingIssues.collect { issue ->
            def versionNumber = 0L

            def version = this.jira.getTextFieldsOfIssue(issue.key as String, [documentVersionField])?.getAt(documentVersionField)
            if (version) {
                try {
                    versionNumber = version.toLong()
                } catch (NumberFormatException _) {
                    this.log.warn("Document tracking issue '${issue.key}' does not contain a valid numerical" +
                        " version. It contains value '${version}'.")
                }
            }

            return versionNumber
        }

        def result = versionList.max()
        log.debug("Retrieved max doc version ${versionList.max()} from doc tracking issues " +
            "${trackingIssues.collect { it.key } }")

        return result
    }

    private void walkTestIssuesAndTestResults(List testIssues, Map testResults, Closure visitor) {
        testResults.testsuites.each { testSuite ->
            testSuite.testcases.each { testCase ->
                def testIssue = testIssues.find { testIssue ->
                    this.checkTestsIssueMatchesTestCase(testIssue, testCase)
                }

                def isMatch = testIssue != null
                visitor(testIssue, testCase, isMatch)
            }
        }
    }

}
