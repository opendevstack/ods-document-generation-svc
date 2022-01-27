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

    // file name used to write token secret yaml
    static final String BB_TOKEN_SECRET = '.bb-token-secret.yml'

    final def script

    // Bae URL of Bitbucket server, such as "https://bitbucket.example.com".
    final String bitbucketUrl

    // Name of Bitbucket project, such as "foo".
    // This name is also the prefix for OpenShift projects ("foo-cd", "foo-dev", ...).
    final String project

    // Name of the CD project in OpenShift, based on the "project" name.
    private final String openShiftCdProject

    // Name of the secret in "openShiftCdProject", which contains the username/token
    // of a user with access to the BitBucket server identified by "bitbucketUrl".
    // This secret does not need to exist ahead of time, it will be created
    // automatically through the API using "passwordCredentialsId".
    private final String tokenSecretName

    // Name of the credentials which store the username/token of a user with
    // access to the BitBucket server identified by "bitbucketUrl".
    // These credentials do not need to exist ahead of time, it will be created
    // automatically (by syncing the "tokenSecretName").
    private String tokenCredentialsId

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
        this.openShiftCdProject = "${project}-cd"
        this.tokenSecretName = 'cd-user-bitbucket-token'

    }

    static String userTokenSecretYml(String tokenSecretName, String username, String password) {
        """\
          apiVersion: v1
          data:
            username: ${AuthUtil.base64(username)}
            password: ${AuthUtil.base64(password)}
          kind: Secret
          type: kubernetes.io/basic-auth
          metadata:
            name: ${tokenSecretName}
            labels:
              credential.sync.jenkins.openshift.io: 'true'
        """.stripIndent()
    }

    String getUrl() {
        bitbucketUrl
    }

    Map getDefaultReviewerConditions(String repo) {
        def response = Unirest.get("${this.baseURL}/rest/default-reviewers/1.0/projects/${projectKey}/repos/${repo}/conditions")
                .routeParam('projectKey', project.toUpperCase())
                .basicAuth(this.username, this.password)
                .header('Accept', 'application/json')
                .asString()

        response.ifFailure {
            def message = "Error: unable to get Bitbucket reviewer conditions. Bitbucket responded with code: '${response.getStatus()}' and message: '${response.getBody()}'."

            if (response.getStatus() == 404) {
                message = "Error: unable to get Bitbucket issue types. Bitbucket could not be found at: '${this.baseURL}'."
            }

            throw new RuntimeException(message)
        }

        return new JsonSlurperClassic().parseText(response.getBody())
    }

    // Returns a list of bitbucket user names (not display names)
    // that are listed as the default reviewers of the given repo.
    List<String> getDefaultReviewers(String repo) {
        List reviewerConditions
        try {
            reviewerConditions = script.readJSON(text: getDefaultReviewerConditions(repo))
        }
        catch (Exception ex) {
            log.warn "Could not understand API response. Error was: ${ex}"
            return []
        }
        List<String> reviewers = []
        for (condition in reviewerConditions) {
            for (reviewer in condition['reviewers']) {
                reviewers.add(reviewer['name'])
            }
        }
        return reviewers
    }

    // Creates pull request in "repo" from branch "fromRef" to "toRef". "reviewers" is a list of bitbucket user names.
    String createPullRequest(String repo, String fromRef, String toRef, String title, String description,
                             List<String> reviewers) {
        String res
        def payload = """{
                "title": "${title}",
                "description": "${description}",
                "state": "OPEN",
                "open": true,
                "closed": false,
                "fromRef": {
                    "id": "refs/heads/${fromRef}",
                    "repository": {
                        "slug": "${repo}",
                        "name": null,
                        "project": {
                            "key": "${project}"
                        }
                    }
                },
                "toRef": {
                    "id": "refs/heads/${toRef}",
                    "repository": {
                        "slug": "${repo}",
                        "name": null,
                        "project": {
                            "key": "${project}"
                        }
                    }
                },
                "locked": false,
                "reviewers": [${reviewers ? reviewers.collect { "{\"user\": { \"name\": \"${it}\" }}" }.join(',') : ''}]
            }"""
        withTokenCredentials { username, token ->
            res = script.sh(
                label: 'Create pull request via API',
                script: """curl \\
                  --fail \\
                  -sS \\
                  --request POST \\
                  --header \"Authorization: Bearer ${token}\" \\
                  --header \"Content-Type: application/json\" \\
                  --data '${payload}' \\
                  ${bitbucketUrl}/rest/api/1.0/projects/${project}/repos/${repo}/pull-requests""",
                returnStdout: true
            ).trim()
        }
        res
    }

    // Get pull requests of "repo" in given "state" (can be OPEN, DECLINED or MERGED).
    String getPullRequests(String repo, String state = 'OPEN') {
        String res
        withTokenCredentials { username, token ->
            res = script.sh(
                label: 'Get pullrequests via API',
                script: """curl \\
                  --fail \\
                  -sS \\
                  --header \"Authorization: Bearer ${token}\" \\
                  ${bitbucketUrl}/rest/api/1.0/projects/${project}/repos/${repo}/pull-requests?state=${state}""",
                returnStdout: true
            ).trim()
        }
        res
    }

    Map findPullRequest(String repo, String branch, String state = 'OPEN') {
        def apiResponse = getPullRequests(repo, state)
        def prCandidates = []
        try {
            def js = new JsonSlurper().parseText(apiResponse)
            prCandidates = js['values']
            if (prCandidates == null) {
                throw new RuntimeException('Field "values" of JSON response must not be empty!')
            }
        } catch (Exception ex) {
            log.warn "Could not understand API response. Error was: ${ex}"
            return [:]
        }
        for (def i = 0; i < prCandidates.size(); i++) {
            def prCandidate = prCandidates[i]
            try {
                def prFromBranch = prCandidate['fromRef']['displayId']
                if (prFromBranch == branch) {
                    return [
                        key: prCandidate['id'],
                        base: prCandidate['toRef']['displayId'],
                    ]
                }
            } catch (Exception ex) {
                log.warn "Unexpected API response. Error was: ${ex}"
                return [:]
            }
        }
        return [:]
    }

    @SuppressWarnings('LineLength')
    void postComment(String repo, int pullRequestId, String comment) {
        withTokenCredentials { username, token ->
            def payload = """{"text":"${comment}"}"""
            script.sh(
                label: "Post comment to PR#${pullRequestId}",
                script: """curl \\
                  --fail \\
                  -sS \\
                  --request POST \\
                  --header \"Authorization: Bearer ${token}\" \\
                  --header \"Content-Type: application/json\" \\
                  --data '${payload}' \\
                  ${bitbucketUrl}/rest/api/1.0/projects/${project}/repos/${repo}/pull-requests/${pullRequestId}/comments"""
            )
        }
    }

    // https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-git-rest.html
    // Creates a tag in the specified repository.
    //The authenticated user must have an effective REPO_WRITE permission to call this resource.
    //
    //'LIGHTWEIGHT' and 'ANNOTATED' are the two type of tags that can be created.
    // The 'startPoint' can either be a ref or a 'commit'.
    void postTag(String repo, String startPoint, String tag, Boolean force = true, String message = "") {
        withTokenCredentials { username, token ->
            def payload = """{
                                     "force": "${force}",
                                     "message": "${message}",
                                     "name": "${tag}",
                                     "startPoint": "${startPoint}",
                                     "type": ${message == '' ? 'LIGHTWEIGHT' : 'ANNOTATED'}
                                 }"""
            script.sh(
                label: "Post git tag to branch",
                script: """curl \\
                      --fail \\
                      -sS \\
                      --request POST \\
                      --header \"Authorization: Bearer ${token}\" \\
                      --header \"Content-Type: application/json\" \\
                      --data '${payload}' \\
                      ${bitbucketUrl}/api/1.0/projects/${project}/repos/${repo}/tags"""
            )
        }
    }

    @SuppressWarnings('LineLength')
    void setBuildStatus(String buildUrl, String gitCommit, String state, String buildName) {
        log.debugClocked("buildstatus-${buildName}-${state}",
            "Setting Bitbucket build status to '${state}' on commit '${gitCommit}' / '${buildUrl}'")
        withTokenCredentials { username, token ->
            def maxAttempts = 3
            def retries = 0
            def payload = "{\"state\":\"${state}\",\"key\":\"${buildName}\",\"name\":\"${buildName}\",\"url\":\"${buildUrl}\"}"
            while (retries++ < maxAttempts) {
                try {
                    script.sh(
                        label: 'Set bitbucket build status via API',
                        script: """curl \\
                            --fail \\
                            -sS \\
                            --request POST \\
                            --header \"Authorization: Bearer ${token}\" \\
                            --header \"Content-Type: application/json\" \\
                            --data '${payload}' \\
                            ${bitbucketUrl}/rest/build-status/1.0/commits/${gitCommit}"""
                    )
                    return
                } catch (err) {
                    log.warn("Could not set Bitbucket build status to '${state}' due to: ${err}")
                }
            }
        }
        log.debugClocked("buildstatus-${buildName}-${state}")
    }

    /**
     * Creates a code insight report in bitbucket via API.
     * For further information visit https://developer.atlassian.com/server/bitbucket/how-tos/code-insights/
     *
     * @param result One of: PASS, FAIL
     */
    void createCodeInsightReport(Map data, String repo, String gitCommit) {
        withTokenCredentials { username, token ->
            def payload = "{" +
                "\"title\":\"${data.title}\"," +
                "\"reporter\":\"OpenDevStack\"," +
                "\"createdDate\":${System.currentTimeMillis()}," +
                "\"details\":\"${data.details}\"," +
                "\"result\":\"${data.result}\","
            if (data.link) {
                payload += "\"link\":\"${data.link}\","
            }
            payload += "\"data\": ["
            data.otherLinks.eachWithIndex { Map link, i ->
                payload += "{" +
                    "\"title\":\"${link.title}\"," +
                    "\"value\":{\"linktext\":\"${link.text}\",\"href\":\"${link.link}\"}," +
                    "\"type\":\"LINK\"" +
                    "}"
                if (i !=  data.otherLinks.size() - 1 || data.messages) {
                    payload += ','
                }
            }
            data.messages.eachWithIndex { Map message, i ->
                payload += "{" +
                    "\"title\":\"${message.title}\"," +
                    "\"value\":\"${message.value}\"," +
                    "\"type\":\"TEXT\"" +
                    "}"
                if (i != data.messages.size() - 1) {
                    payload += ','
                }
            }
            payload += "]" +
                "}"
            try {
                script.sh(
                    label: 'Create Bitbucket Code Insight report via API',
                    script: """curl \\
                        --fail \\
                        -sS \\
                        --request PUT \\
                        --header \"Authorization: Bearer ${token}\" \\
                        --header \"Content-Type: application/json\" \\
                        --data '${payload}' \\
                        ${bitbucketUrl}/rest/insights/1.0/projects/${project}/\
repos/${repo}/commits/${gitCommit}/reports/${data.key}"""
                )
                return
            } catch (err) {
                log.warn("Could not create Bitbucket Code Insight report due to: ${err}")
            }
        }
    }

    @SuppressWarnings('SynchronizedMethod')
    private synchronized void createUserTokenIfMissing() {
        def credentialsId = "${openShiftCdProject}-${tokenSecretName}"

        if (basicAuthCredentialsIdExists(credentialsId)) {
            log.debug "Secret ${tokenSecretName} exists and is synced."
            this.tokenCredentialsId = credentialsId
            return
        }

        def credentialsAvailable = false
        log.info "Secret ${tokenSecretName} does not exist yet, it will be created now."
        def token = createUserToken()
        if (token['password']) {
            createUserTokenSecret(token['username'], token['password'])
        }

        // Ensure that secret is synced to Jenkins before continuing.
        def waitTime = 10 // seconds
        def retries = 3
        for (def i = 0; i < retries; i++) {
            if (basicAuthCredentialsIdExists(credentialsId)) {
                credentialsAvailable = true
                break
            } else {
                log.debug "Waiting ${waitTime} for credentials '${credentialsId}' to become available."
                script.sleep(waitTime)
                waitTime = waitTime * 2
            }
        }
        if (credentialsAvailable) {
            this.tokenCredentialsId = credentialsId
        } else {
            throw new RuntimeException(
                "ERROR: Secret ${openShiftCdProject}/${tokenSecretName} has been created, " +
                    "but credentials '${credentialsId}' are not available. " +
                    'Please ensure that the secret is synced and re-run the pipeline.'
            )
        }
    }

    @SuppressWarnings('LineLength')
    Map<String, String> createUserToken() {
        Map<String, String> tokenMap = [username: '', password: '']
        def res = ''
        def payload = """{"name": "ods-jenkins-shared-library-${openShiftCdProject}", "permissions": ["PROJECT_WRITE", "REPO_WRITE"]}"""

            tokenMap['username'] = username
            String url = "${bitbucketUrl}/rest/access-tokens/1.0/users/${username.replace('@', '_')}"
            script.echo "Requesting token via PUT ${url} with payload=${payload}"
            res = script.sh(
                returnStdout: true,
                script: """set +x; curl \\
                  --fail \\
                  -sS \\
                  --request PUT \\
                  --header \"Content-Type: application/json\" \\
                  --header \"${AuthUtil.header(AuthUtil.SCHEME_BASIC, username, password)}\" \\
                  --data '${payload}' \\
                  ${url}
                """
            ).trim()
            try {
                def js =  new JsonSlurper().parseText(res)
                String token = js['token']
                tokenMap['password'] = token
            } catch (Exception ex) {
                log.warn "Could not understand API response. Error was: ${ex}"
            }

        return tokenMap
    }

    Map getCommitsForIntegrationBranch(String token, String repo, int limit, int nextPageStart){
        String request = "${bitbucketUrl}/rest/api/1.0/projects/${project}/repos/${repo}/commits"
        return queryRepo(token, request, limit, nextPageStart)
    }

    
    Map getPRforMergedCommit(String token, String repo, String commit) {
        String request = "${bitbucketUrl}/rest/api/1.0/projects/${project}" +
            "/repos/${repo}/commits/${commit}/pull-requests"
        return queryRepo(token, request, 0, 0)
    }

    
    private Map queryRepo(String token, String request, int limit, int nextPageStart) {
        Map<String, String> headers = buildHeaders(token)
        def httpRequest = Unirest.get(request).headers(headers)
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

    
    private Map<String, String> buildHeaders(String token) {
        Map<String, String> headers = [:]
        headers.put("accept", "application/json")
        headers.put("Authorization", "Bearer ".concat(token))
        return headers
    }

    private void createUserTokenSecret(String username, String password) {
        String secretYml = userTokenSecretYml(tokenSecretName, username, password)
        script.writeFile(
            file: BB_TOKEN_SECRET,
            text: secretYml
        )
        try {
            script.sh """
                oc -n ${openShiftCdProject} create -f ${BB_TOKEN_SECRET};
                rm ${BB_TOKEN_SECRET}
            """
        } catch (Exception ex) {
            log.warn "Could not create secret ${tokenSecretName}. Error was: ${ex}"
        }
    }

}
