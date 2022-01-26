package org.ods.shared.lib.jira

import groovy.util.logging.Slf4j
import org.ods.doc.gen.project.data.Project
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.stereotype.Service

import javax.inject.Inject

@SuppressWarnings(['IfStatementBraces', 'LineLength'])
@Slf4j
@Service
class JiraUseCase {

    private final Project project
    private final JiraService jira

    @Inject
    JiraUseCase(Project project, JiraService jira) {
        this.project = project
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
