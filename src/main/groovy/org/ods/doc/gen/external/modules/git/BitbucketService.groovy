package org.ods.doc.gen.external.modules.git

import feign.FeignException
import feign.Response
import feign.codec.ErrorDecoder
import groovy.json.JsonSlurperClassic
import groovy.util.logging.Slf4j
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Slf4j
@Service
class BitbucketService {

    private ZipFacade zipFacade
    private final BitBucketClientConfig bitBucketClientConfig

    @Inject
    BitbucketService(BitBucketClientConfig bitBucketClientConfig,
                     ZipFacade zipFacade) {
        this.bitBucketClientConfig = bitBucketClientConfig
        this.zipFacade = zipFacade
    }

    Map getCommitsForIntegrationBranch(String repo, ProjectData projectData, int nextPageStart){
        String projectKey = projectData.getKey()
        String response = bitBucketClientConfig.getClient().getCommitsForDefaultBranch(projectKey, repo, nextPageStart)
        return new JsonSlurperClassic().parseText(response)
    }

    Map getPRforMergedCommit(String repo, ProjectData projectData, String commit) {
        String projectKey = projectData.getKey()
        String response = bitBucketClientConfig.getClient().getPRforMergedCommit(projectKey, repo, commit)
        return new JsonSlurperClassic().parseText(response)
    }

    String buildRepositoryUrl(String projectId, String repositoryName) {
        URI uri = new URI([getBitbucketURLForDocs(), projectId, repositoryName].join("/"))
        return "${uri.normalize().toString()}.git"
    }

    String getBitbucketURLForDocs() {
        return bitBucketClientConfig.url
    }

    void downloadRepo(String project, String repo, String branch, String tmpFolder) {
        downloadRepoWithFallBack(project, repo, branch, null, tmpFolder)
    }

    void downloadRepoWithFallBack(String project, String repo, String branch, String defaultBranch,
                                            String tmpFolder) {
        log.info("downloadRepo: project:${project}, repo:${repo} and branch:${branch}")
        Path zipArchive = Files.createTempFile("archive-", ".zip")
        try {
            try {
                bitBucketClientConfig
                        .getClient()
                        .getRepoZipArchive(project, repo, branch)
                        .withCloseable { Response response ->
                            streamResult(response, zipArchive)
                        }
            } catch (Exception firstTryException) {
                if (defaultBranch) {
                    log.warn("Branch [${branch}] doesn't exist, using branch: [${defaultBranch}]")
                    bitBucketClientConfig
                            .getClient()
                            .getRepoZipArchive(project, repo, defaultBranch)
                            .withCloseable { Response response ->
                                streamResult(response, zipArchive)
                            }
                } else {
                    throw firstTryException
                }
            }
            zipFacade.extractZipArchive(zipArchive, Paths.get(tmpFolder))
        } catch (FeignException callException) {
            checkError(repo, branch, callException)
        } finally {
            Files.delete(zipArchive)
        }

    }

    void downloadRepoMetadata(String project, String repo, String branch, String defaultBranch, String tmpFolder) {
        log.info("downloadRepo: project:${project}, repo:${repo} and branch:${branch}")
        Path zipArchive = Files.createTempFile("archive-${repo}", ".zip")
        try {
            try {
                bitBucketClientConfig
                        .getClient()
                        .getRepoFileInZipArchive(project, repo, branch, "metadata.yml")
                        .withCloseable { Response response ->
                            streamResult(response, zipArchive)
                        }
            } catch (e) {
                log.warn("Branch [${branch}] doesn't exist, using branch: [${defaultBranch}]")
                bitBucketClientConfig
                        .getClient()
                        .getRepoFileInZipArchive(project, repo, defaultBranch, "metadata.yml")
                        .withCloseable { Response response ->
                            streamResult(response, zipArchive)
                        }
            }

            zipFacade.extractZipArchive(zipArchive, Paths.get(tmpFolder))
        } catch (FeignException callException) {
            checkError(repo, branch, callException)
        } finally {
            Files.delete(zipArchive)
        }
    }

    protected void streamResult(Response response, Path zipArchive){
        if (response.status() >= 300) {
            throw new ErrorDecoder.Default().decode('downloadRepo', response)
        }
        response.body().withCloseable { body ->
            body.asInputStream().withStream { is ->
                Files.copy(is, zipArchive, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    protected void checkError(repo, String branch, FeignException callException) {
        def baseErrMessage = "Could not get document zip from '${repo}'!- For version:${branch}"
        if (callException instanceof FeignException.BadRequest) {
            throw new RuntimeException("${baseErrMessage} \rIs there a correct release branch configured?", callException)
        } else if (callException instanceof FeignException.Unauthorized) {
            throw new RuntimeException("${baseErrMessage} \rDoes '${bitBucketClientConfig.username}' have access?", callException)
        } else if (callException instanceof FeignException.NotFound) {
            throw new RuntimeException("${baseErrMessage}", callException)
        } else {
            throw callException
        }
    }

}
