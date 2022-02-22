package org.ods.doc.gen.external.modules.git.fixtureDatas

import feign.Feign
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.auth.BasicAuthRequestInterceptor
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

import javax.inject.Inject

interface CheckRepoExistsHttpApi {
    @Headers("Accept: application/json")
    @RequestLine("GET /rest/api/latest/projects/{project}/repos/{repo}")
    String check(@Param("project") String project, @Param("repo") String repo)
}

@Slf4j
@Service
class CheckRepoExists {

    URI baseURL
    String username
    String password

    @Inject
    CheckRepoExists(@Value('${bitbucket.url}') String bbBaseURL,
                    @Value('${bitbucket.username}')  String username,
                    @Value('${bitbucket.password}') String password) {
        this.username = username
        this.password = password

        if (bbBaseURL.endsWith('/')) {
            bbBaseURL = bbBaseURL.substring(0, bbBaseURL.size() - 1)
        }

        try {
            this.baseURL = new URIBuilder(bbBaseURL).build()
        } catch (e) {
            throw new IllegalArgumentException("Error: unable to connect to Jira. '${bbBaseURL}' is not a valid URI.").initCause(e)
        }
    }

    private void checkRepoExists(Map data) {

        Feign.Builder builder = Feign.builder()
        if (username && password) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(username, password))
        }

        String url = baseURL.getScheme() + "://" + baseURL.getAuthority()
        CheckRepoExistsHttpApi checkRepoExistsHttpApi = builder.target(CheckRepoExistsHttpApi.class, url)
        checkRepoExistsHttpApi.check(data.id, data.releaseRepo)
    }
}
