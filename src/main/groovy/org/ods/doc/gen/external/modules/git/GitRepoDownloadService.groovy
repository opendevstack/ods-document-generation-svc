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

interface GitRepoVersionHttpAPI extends GitRepoHttpAPI {
    @Headers("Accept: application/octet-stream")
    @RequestLine("GET /rest/api/latest/projects/{documentTemplatesProject}/repos/{documentTemplatesRepo}/archive?at=refs/heads/release/v{version}&format=zip")
    byte[] getRepoZipArchive(@Param("documentTemplatesProject") String documentTemplatesProject, @Param("documentTemplatesRepo") String documentTemplatesRepo, @Param("version") String version)
}

interface GitRepoBranchHttpAPI extends GitRepoHttpAPI {
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

    void getRepoContentsToFolder(String project, String repo, String version, String targetFolderRelativePath, GitRepoVersionType versionType) {
        def targetDir = Paths.get(targetFolderRelativePath)
        GitRepoHttpAPI store = createStorageClient(versionType)
        byte[] zipArchiveContent = getZipArchiveFromStore(store, project, repo, version)
        zipFacade.extractZipArchive(zipArchiveContent, targetDir)
    }

    private byte[] getZipArchiveFromStore(GitRepoHttpAPI store, String project, String repo, String version) {
        try {
            return store.getRepoZipArchive(project, repo, version)
        } catch (FeignException callException) {
            def baseErrMessage = "Could not get document zip from '${this.baseURL}'!"
            def baseRepoErrMessage = "${baseErrMessage}\rIn repository '${repo}' - "
            if (callException instanceof FeignException.BadRequest) {
                throw new RuntimeException("${baseRepoErrMessage}" +
                        "is there a correct release branch configured, called 'release/v${version}'?")
            } else if (callException instanceof FeignException.Unauthorized) {
                def bbUserNameError = this.username ?: 'Anyone'
                throw new RuntimeException("${baseRepoErrMessage} \rDoes '${bbUserNameError}' have access?")
            } else if (callException instanceof FeignException.NotFound) {
                throw new RuntimeException("${baseErrMessage}" +
                        "\rDoes repository '${repo}' in project: '${project}' exist?")
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
            return builder.target(GitRepoBranchHttpAPI.class, this.baseURL.getScheme() + "://" + this.baseURL.getAuthority())
        }
        return builder.target(GitRepoVersionHttpAPI.class, this.baseURL.getScheme() + "://" + this.baseURL.getAuthority())
    }
}