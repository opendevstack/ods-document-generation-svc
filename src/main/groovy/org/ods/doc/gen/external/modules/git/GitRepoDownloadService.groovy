package org.ods.doc.gen.external.modules.git

import feign.Feign
import feign.FeignException
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.auth.BasicAuthRequestInterceptor
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.core.ZipFacade
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Paths


public enum GitRepoVersionType { BRANCH, COMMIT }

interface GitRepoHttpAPI {
    abstract byte[] getRepoZipArchive(@Param("documentTemplatesProject") String documentTemplatesProject, @Param("documentTemplatesRepo") String documentTemplatesRepo, @Param("version") String version)
}

/** Examples for version parameter:
 * version = refs/heads/release/v{hash}
 * version = refs/tags/CHG0066328
 */
interface GitRepoVersionDownloadHttpAPI extends GitRepoHttpAPI {
    @Headers("Accept: application/octet-stream")
    @RequestLine("GET /rest/api/latest/projects/{documentTemplatesProject}/repos/{documentTemplatesRepo}/archive?at={version}&format=zip")
    byte[] getRepoZipArchive(@Param("documentTemplatesProject") String documentTemplatesProject, @Param("documentTemplatesRepo") String documentTemplatesRepo, @Param("version") String version)
}

/** Examples for branch parameter:
 * branch = master
 */
interface GitRepoBranchDownloadHttpAPI extends GitRepoHttpAPI {
    @Headers("Accept: application/octet-stream")
    @RequestLine("GET /rest/api/latest/projects/{documentTemplatesProject}/repos/{documentTemplatesRepo}/archive?at=refs/heads/{branch}&format=zip")
    byte[] getRepoZipArchive(@Param("documentTemplatesProject") String documentTemplatesProject, @Param("documentTemplatesRepo") String documentTemplatesRepo, @Param("branch") String branch)
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
        GitRepoVersionType versionType = GitRepoVersionType.COMMIT
        GitRepoHttpAPI store = createStorageClient(versionType)
        byte[] zipArchiveContent = getZipArchiveFromStore(store, data, versionType)
        zipFacade.extractZipArchive(zipArchiveContent, targetDir)
    }

    private byte[] getZipArchiveFromStore(GitRepoHttpAPI store, Map data, GitRepoVersionType versionType) {

        String url = data.git.url
        String [] urlPieces = url.split('/')
        String project = urlPieces[urlPieces.length -2]
        String repo = urlPieces[urlPieces.length -1]
        String version = data.git.releaseManagerBranch

        try {
            return store.getRepoZipArchive(project, repo, version)
        } catch (FeignException callException) {
            def baseErrMessage = "Could not get document zip from '${this.baseURL}'!"
            def baseRepoErrMessage = "${baseErrMessage}\rIn repository '${repo}' - "
            if (callException instanceof FeignException.BadRequest) {
                def errorMsg =
                        "${baseRepoErrMessage}" + "is there a correct release branch configured, called 'release/v${version}'?"
                log.error(errorMsg)
                throw new RuntimeException()
            } else if (callException instanceof FeignException.Unauthorized) {
                def bbUserNameError = this.username ?: 'Anyone'
                def errorMsg = "${baseRepoErrMessage} \rDoes '${bbUserNameError}' have access?"
                log.error(errorMsg)
                throw new RuntimeException(errorMsg)
            } else if (callException instanceof FeignException.NotFound) {
                def errorMsg = "${baseErrMessage}" + "\rDoes repository '${repo}' in project: '${project}' exist?"
                log.error(errorMsg)
                throw new RuntimeException(errorMsg)
            } else {
                throw callException
            }
        }
    }

    private GitRepoHttpAPI createStorageClient(GitRepoVersionType versionType) {
        Feign.Builder builder = Feign.builder()
        if (this.username && this.password) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(this.username, this.password))
        }

        if (GitRepoVersionType.BRANCH == versionType) {
            return builder.target(GitRepoBranchDownloadHttpAPI.class, this.baseURL.getScheme() + "://" + this.baseURL.getAuthority())
        }
        return builder.target(GitRepoVersionDownloadHttpAPI.class, this.baseURL.getScheme() + "://" + this.baseURL.getAuthority())
    }
}
