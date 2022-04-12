package org.ods.doc.gen.leva.doc.test.doubles

import groovy.util.logging.Slf4j
import org.ods.doc.gen.external.modules.jira.JiraService
import org.springframework.beans.factory.annotation.Value
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
    void updateTextFieldsOnIssue(String issueIdOrKey, Map fields) {
        log.warn("updateTextFieldsOnIssue - issueIdOrKey:$issueIdOrKey")
    }

    void setBaseURL(URI jiraNewBaseUrl) {
        log.info("Updating Jira baseUrl to ${jiraNewBaseUrl}")
        super.baseURL = jiraNewBaseUrl
        log.info("Updated Jira baseUrl to ${super.baseURL}")
    }

    Map getFileFromJira(String url) {
        url = replaceHostInUrlWithBaseUrl(url)
        return super.getFileFromJira(url)
    }

    private String replaceHostInUrlWithBaseUrl(String url) {
        url = url.replace("http://", "").replace("https://", "")
        int pos = url.indexOf("/")
        url = url.substring(pos)
        url = super.baseURL.toString() + url
        return url
    }
}
