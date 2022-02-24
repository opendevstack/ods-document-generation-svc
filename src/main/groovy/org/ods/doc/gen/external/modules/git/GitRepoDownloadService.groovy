package org.ods.doc.gen.external.modules.git

import feign.Feign
import feign.FeignException
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.auth.BasicAuthRequestInterceptor
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.leva.doc.api.LevaDocType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson

interface GitRepoHttpAPI {
    abstract byte[] getRepoZipArchive(@Param("documentTemplatesProject") String documentTemplatesProject, @Param("documentTemplatesRepo") String documentTemplatesRepo, @Param("version") String version)
}

/** Examples for version parameter:
 * version = refs/heads/release/{hash}
 * version = refs/tags/CHG0066328
 * version = refs/heads/master
 */
interface GitRepoVersionDownloadHttpAPI extends GitRepoHttpAPI {
    @Headers("Accept: application/octet-stream")
    @RequestLine("GET /rest/api/latest/projects/{documentTemplatesProject}/repos/{documentTemplatesRepo}/archive?at={version}&format=zip")
    byte[] getRepoZipArchive(@Param("documentTemplatesProject") String documentTemplatesProject, @Param("documentTemplatesRepo") String documentTemplatesRepo, @Param("version") String version)
}

@SuppressWarnings(['PublicMethodsBeforeNonPublicMethods'])
@Slf4j
@Service
class GitRepoDownloadService {

    // Username and password we use to connect to bitbucket
    String username
    String password

    // Bitbucket base url
    URI baseURL

    // Utils
    private ZipFacade zipFacade

    @Inject
    GitRepoDownloadService(@Value('${bitbucket.url}') String baseURL,
                           @Value('${bitbucket.username}')  String username,
                           @Value('${bitbucket.password}') String password,
                           ZipFacade zipFacade) {
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
        this.zipFacade = zipFacade
    }

    void getRepoContentsToFolder(Map data, String targetFolderRelativePath) {
        def targetDir = Paths.get(targetFolderRelativePath)
        GitRepoHttpAPI store = createStorageClient()
        byte[] zipArchiveContentBytes = getZipArchiveFromStore(store, data)
        Path zipArchive = Files.createTempFile("release-manager-repo-content", ".zip")
        zipArchive.append(zipArchiveContentBytes)
        zipFacade.extractZipArchive(zipArchive, targetDir)
    }

    private byte[] getZipArchiveFromStore(GitRepoHttpAPI store, Map data) {

        String repoURL = data.git.repoURL
        String [] urlPieces = repoURL.split('/')
        String project = urlPieces[urlPieces.length -2]
        String repo = urlPieces[urlPieces.length -1]
        String releaseManagerBranch = data.git.releaseManagerBranch

        if (StringUtils.isEmpty(repoURL)) {
            logData(data);
            throw new RuntimeException("Value for Git repoURL is empty or null.")
        }
        if (StringUtils.isEmpty(releaseManagerBranch)) {
            logData(data);
            throw new RuntimeException("Value for Git releaseManagerBranch is empty or null.")
        }

        try {
            return store.getRepoZipArchive(project, repo, releaseManagerBranch)
        } catch (FeignException callException) {
            def baseErrMessage = "Could not get document zip from '${this.baseURL}'!"
            def baseRepoErrMessage = "${baseErrMessage}\rIn repository '${repo}' - "
            if (callException instanceof FeignException.BadRequest) {
                def errorMsg =
                        "${baseRepoErrMessage}" + "is there a correct release branch configured, called '${releaseManagerBranch}'?"
                log.error(errorMsg, callException)
                throw new RuntimeException(callException)
            } else if (callException instanceof FeignException.Unauthorized) {
                def bbUserNameError = this.username ?: 'Anyone'
                def errorMsg = "${baseRepoErrMessage} \rDoes '${bbUserNameError}' have access?"
                log.error(errorMsg, callException)
                throw new RuntimeException(callException)
            } else if (callException instanceof FeignException.NotFound) {
                def errorMsg = "${baseErrMessage}" + "\rDoes repository '${repo}' in project: '${project}' exist?"
                log.error(errorMsg, callException)
                throw new RuntimeException(callException)
            } else {
                throw callException
            }
        }
    }

    private GitRepoHttpAPI createStorageClient() {
        Feign.Builder builder = Feign.builder()
        if (this.username && this.password) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(this.username, this.password))
        }

        return builder.target(GitRepoVersionDownloadHttpAPI.class, this.baseURL.getScheme() + "://" + this.baseURL.getAuthority())
    }

    private static void logData(Map body) {
        log.debug(prettyPrint(toJson(body)))
    }
}
