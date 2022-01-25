package org.ods.doc.gen.core.test.jira

import groovy.util.logging.Slf4j
import  org.ods.shared.lib.jira.JiraService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Slf4j
@Profile("test")
@Service
class JiraServiceForWireMock extends JiraService {

    JiraServiceForWireMock(@Value('${jira.url}') String baseURL,
                           @Value('${jira.username}')  String username,
                           @Value('${jira.password}') String password) {
        super(baseURL, username, password)
    }

    @Override
    void appendCommentToIssue(String issueIdOrKey, String comment) {
        log.warn("appendCommentToIssue - issueIdOrKey:$issueIdOrKey")
    }

    @Override
    Map createIssueType(String type, String projectKey, String summary, String description, String fixVersion = null) {
        log.warn("createIssueType - type:${type}")
    }

    @Override
    void updateTextFieldsOnIssue(String issueIdOrKey, Map fields) {
        log.warn("updateTextFieldsOnIssue - issueIdOrKey:$issueIdOrKey")
    }
}
