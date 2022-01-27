package org.ods.doc.gen.external.modules.git

import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic
import groovy.util.logging.Slf4j
import kong.unirest.Unirest
import org.apache.http.client.utils.URIBuilder
import org.ods.shared.lib.jenkins.AuthUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import javax.inject.Inject

@SuppressWarnings(['PublicMethodsBeforeNonPublicMethods', 'ParameterCount'])
@Slf4j
@Service
class BitbucketService {

    // Bae URL of Bitbucket server, such as "https://bitbucket.example.com".
    final String bitbucketUrl

    // Name of Bitbucket project, such as "foo".
    // This name is also the prefix for OpenShift projects ("foo-cd", "foo-dev", ...).
    final String project

    // Username and password we use to connect to bitbucket
    String username
    String password

    // Bitbucket base url
    URI baseURL

    BitbucketService(@Value('${bitbucket.url}') String baseURL,
                @Value('${bitbucket.username}')  String username,
                @Value('${bitbucket.password}') String password) {
        if (!baseURL?.trim()) {
            throw new IllegalArgumentException('Error: unable to connect to Jira. \'baseURL\' is undefined.')
        }

        if (!username?.trim()) {
            throw new IllegalArgumentException('Error: unable to connect to Jira. \'username\' is undefined.')
        }

        if (!password?.trim()) {
            throw new IllegalArgumentException('Error: unable to connect to Jira. \'password\' is undefined.')
        }

        if (baseURL.endsWith('/')) {
            baseURL = baseURL.substring(0, baseURL.size() - 1)
        }

        try {
            this.baseURL = new URIBuilder(baseURL).build()
        } catch (e) {
            throw new IllegalArgumentException("Error: unable to connect to Jira. '${baseURL}' is not a valid URI.").initCause(e)
        }

        this.username = username
        this.password = password

        this.project = "FRML24113" // TODO s2o
    }

    String getUrl() {
        bitbucketUrl
    }


    Map getCommitsForIntegrationBranch(String repo, int limit, int nextPageStart){
        String request = "${bitbucketUrl}/rest/api/1.0/projects/${project}/repos/${repo}/commits"
        return queryRepo(request, limit, nextPageStart)
    }

    
    Map getPRforMergedCommit(String repo, String commit) {
        String request = "${bitbucketUrl}/rest/api/1.0/projects/${project}" +
            "/repos/${repo}/commits/${commit}/pull-requests"
        return queryRepo(request, 0, 0)
    }

    
    private Map queryRepo(String request, int limit, int nextPageStart) {

        def httpRequest = Unirest.get(request)
                .basicAuth(this.username, this.password)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")

        if (limit>0) {
            httpRequest.queryString("limit", limit)
        }
        if (nextPageStart>0) {
            httpRequest.queryString("start", nextPageStart)
        }
        def response = httpRequest.asString()

        response.ifFailure {
            def message = 'Error: unable to get data from Bitbucket responded with code: ' +
                "'${response.getStatus()}' and message: '${response.getBody()}'."
            throw new RuntimeException(message)
        }

        return new JsonSlurperClassic().parseText(response.getBody())
    }

}
