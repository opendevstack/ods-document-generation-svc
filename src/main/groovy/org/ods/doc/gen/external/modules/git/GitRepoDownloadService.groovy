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
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
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

/** Examples for version parameter:
 * version = refs/heads/release/{hash}
 * version = refs/tags/CHG0066328
 * version = refs/heads/master
 */
interface GitRepoDownloadHttpAPI {
    @Headers("Accept: application/octet-stream")
    @RequestLine("GET /rest/api/latest/projects/{project}/repos/{repo}/archive?format=zip&at={version}")
    byte[] getRepoZipArchive(@Param("project") String documentTemplatesProject, @Param("repo") String documentTemplatesRepo, @Param("version") String version)
}

interface GitRepoGetBranchCommits {
    @Headers("Accept: application/json")
    @RequestLine("GET /rest/api/latest/projects/{project}/repos/{repo}/commits?branch={branch}&limit=1")
    String getBranchCommits(@Param("project") String project, @Param("repo") String repo, @Param("branch") String branch)
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

    void getRepoContentsAsZipAndExtractToFolder(Map data, String targetFolderRelativePath) {
        def targetDir = Paths.get(targetFolderRelativePath)
        GitRepoDownloadHttpAPI store = createStorageClient()
        byte[] zipArchiveContentBytes = getZipArchiveFromStore(store, data)
        Path zipArchive = Files.createTempFile("release-manager-repo-content", ".zip")
        zipArchive.append(zipArchiveContentBytes)
        zipFacade.extractZipArchive(zipArchive, targetDir)
    }

    private byte[] getZipArchiveFromStore(GitRepoDownloadHttpAPI store, Map data) {

        String repoURL = data.git.repoURL
        String releaseManagerBranch = data.git.releaseManagerBranch

        if (StringUtils.isEmpty(repoURL)) {
            logData(data);
            throw new IllegalArgumentException("Value for Git repoURL is empty or null.")
        }
        if (StringUtils.isEmpty(releaseManagerBranch)) {
            logData(data);
            throw new IllegalArgumentException("Value for Git releaseManagerBranch is empty or null.")
        }

        String [] urlPieces = repoURL.split('/')
        String project = urlPieces[urlPieces.length -2]
        String repo = urlPieces[urlPieces.length -1]

        repo = repo.replaceFirst("\\.git", "")

        try {
            return store.getRepoZipArchive(project, repo, releaseManagerBranch)
        } catch (FeignException callException) {
            def baseErrMessage = "Could not get document zip from '${this.baseURL}'!"
            def baseRepoErrMessage = "${baseErrMessage}\rIn repository '${repo}' - "
            if (callException instanceof FeignException.BadRequest) {
                def errorMsg =
                        "${baseRepoErrMessage}" + "is there a correct release branch configured, called '${releaseManagerBranch}'? \n"
                log.error(errorMsg, callException)
                throw new RuntimeException(callException)
            } else if (callException instanceof FeignException.Unauthorized) {
                def bbUserNameError = this.username ?: 'Anyone'
                def errorMsg = "${baseRepoErrMessage} \rDoes '${bbUserNameError}' have access? \n"
                log.error(errorMsg, callException)
                throw new RuntimeException(callException)
            } else if (callException instanceof FeignException.NotFound) {
                def errorMsg = "${baseErrMessage}" + "\rDoes repository '${repo}' in project: '${project}' exist? \n"
                log.error(errorMsg, callException)
                throw new RuntimeException(callException)
            } else {
                throw callException
            }
        }
    }

    private GitRepoDownloadHttpAPI createStorageClient() {
        Feign.Builder builder = Feign.builder()
        if (this.username && this.password) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(this.username, this.password))
        } else {
            log.warn("No username/password provided for connecting to BitBucket.")
        }

        return builder.target(GitRepoDownloadHttpAPI.class, this.baseURL.getScheme() + "://" + this.baseURL.getAuthority())
    }

    private static void logData(Map body) {
        log.debug(prettyPrint(toJson(body)))
    }

    void gitCloneReleaseManagerRepo(Map data, String targetFolderRelativePath) {
        File targetDir = new File(targetFolderRelativePath)
        String repoURI = data.git.repoURL
        String branchName = data.git.releaseManagerBranch
        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(username, password);

        if (checkRepositoryBranchExists(data)) {
            Git git = Git.cloneRepository()
                    .setCredentialsProvider(cp)
                    .setURI(repoURI)
                    .setDirectory(targetDir)
                    .setBranch(branchName)
                    .call();
        } else {
            // Create a new branch from master, named as requested.
            Git git = Git.cloneRepository()
                    .setCredentialsProvider(cp)
                    .setURI(repoURI)
                    .setDirectory(targetDir)
                    .setBranch("master")
                    .call();
            git.checkout()
                    .setCreateBranch(true)
                    .setName(branchName)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .setStartPoint("refs/heads/master") // origin/master ??
                    .call()
        }
    }

    boolean checkRepositoryBranchExists(Map data) {
        String repoURL = data.git.repoURL
        String releaseManagerBranch = data.git.releaseManagerBranch

        if (StringUtils.isEmpty(repoURL)) {
            logData(data);
            throw new IllegalArgumentException("Value for Git repoURL is empty or null.")
        }
        if (StringUtils.isEmpty(releaseManagerBranch)) {
            logData(data);
            throw new IllegalArgumentException("Value for Git releaseManagerBranch is empty or null.")
        }

        repoURL = repoURL.replaceFirst("\\.git", "")
        String [] urlPieces = repoURL.split('/')
        String project = urlPieces[urlPieces.length -2]
        String repo = urlPieces[urlPieces.length -1]

        // Remove problematic character from branch name.
        if ((!StringUtils.isEmpty(releaseManagerBranch)) && releaseManagerBranch.contains("/")) {
            releaseManagerBranch = releaseManagerBranch.substring(releaseManagerBranch.lastIndexOf("/") +1)
        }

        Feign.Builder builder = Feign.builder()
        if (this.username && this.password) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(this.username, this.password))
        }

        String serverURI = this.baseURL.getScheme() + "://" + this.baseURL.getAuthority()
        GitRepoGetBranchCommits client = builder.target(GitRepoGetBranchCommits.class, serverURI)

        try {
            String result = client.getBranchCommits(project, repo, releaseManagerBranch)
            log.debug(prettyPrint(toJson(result)))
            return true
        } catch (FeignException callException) {
            log.info("Branch ${releaseManagerBranch} not found in repo ${repo} of project ${project}. Server: ${serverURI}")
            return false
        }
    }
}
