package org.ods.doc.gen.external.modules.git

import feign.FeignException
import feign.Response
import feign.codec.ErrorDecoder
import groovy.json.JsonSlurperClassic
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.GithubClientConfig
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
class GithubService {

    private ZipFacade zipFacade
    private final GithubClientConfig githubClientConfig

    @Inject
    GithubService(GithubClientConfig githubClientConfig,
                  ZipFacade zipFacade) {
        this.githubClientConfig = githubClientConfig
        this.zipFacade = zipFacade
    }

    void downloadRepo(String version, Path tmpFolder) {
        Path zipArchive = Files.createTempFile("archive-", ".zip")
        try {
            githubClientConfig
                    .getClient()
                    .getTemplatesZip(version)
                    .withCloseable { Response response ->
                        streamResult(response, zipArchive)
                    }
            zipFacade.extractZipArchive(zipArchive, tmpFolder)
        } catch (FeignException callException) {
            checkError(version, callException)
        } finally {
            Files.delete(zipArchive)
        }
    }

    private void streamResult(Response response, Path zipArchive){
        if (response.status() >= 300) {
            throw new ErrorDecoder.Default().decode('downloadTemplates', response)
        }
        response.body().withCloseable { body ->
            body.asInputStream().withStream { is ->
                Files.copy(is, zipArchive, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private void checkError(String version, FeignException callException) {
        def baseErrMessage = "Could not get document zip from GH - For version:${version}"
        if (callException instanceof FeignException.BadRequest) {
            throw new RuntimeException("FeignException.BadRequest ${baseErrMessage} \r" +
                    "Is there a correct release branch configured?")
        } else if (callException instanceof FeignException.NotFound) {
            throw new RuntimeException("FeignException.NotFound: ${baseErrMessage}")
        } else {
            throw callException
        }
    }

}
