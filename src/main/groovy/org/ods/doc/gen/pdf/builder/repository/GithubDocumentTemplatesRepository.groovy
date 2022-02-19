package org.ods.doc.gen.pdf.builder.repository

import feign.FeignException
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.Response
import feign.codec.ErrorDecoder
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.GithubClientConfig
import org.ods.doc.gen.core.ZipFacade
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


interface GithubDocumentTemplatesStoreHttpAPI {

    @Headers("Accept: application/octet-stream")
    @RequestLine("GET /opendevstack/ods-document-generation-templates/archive/v{version}.zip")
    Response getTemplatesZipArchiveForVersion(@Param("version") String version)

}

@Slf4j
@Order(1)
@Repository
class GithubDocumentTemplatesRepository implements DocumentTemplatesRepository {

    private final ZipFacade zipFacade
    private final String basePath
    private final GithubClientConfig githubClientConfig

    @Inject
    GithubDocumentTemplatesRepository(GithubClientConfig githubClientConfig, ZipFacade zipFacade, @Value('${cache.documents.basePath}') String basePath){
        this.githubClientConfig = githubClientConfig
        this.zipFacade = zipFacade
        this.basePath = basePath
    }

    Path getTemplatesForVersion(String version) {
        log.info ("getTemplatesForVersion version:${version}")

        def targetDir = Paths.get(basePath, version)
        Path zipArchive = Files.createTempFile("archive-", ".zip")
        try {
            downloadZipArchive(version, zipArchive)
            zipFacade.extractZipArchive(zipArchive, targetDir)
            moveContentToRootFolder(targetDir, "ods-document-generation-templates-${version}")
        } catch (Throwable e) {
            throw e
        } finally {
            Files.delete(zipArchive)
        }

        return targetDir
    }

    boolean isApplicableToSystemConfig () {
        return true
    }

    static URI getURItoDownloadTemplates(String version) {
        String githubUrl = System.getenv("GITHUB_HOST") ?: "https://www.github.com"
        return URI.create("${githubUrl}/opendevstack/ods-document-generation-templates/archive/v${version}.zip")
    }

    private void moveContentToRootFolder(targetDir, startAtDir){
        FileUtils.copyDirectory(new File(targetDir.toFile(), startAtDir), targetDir.toFile())
        FileUtils.deleteDirectory(new File(targetDir.toFile(), startAtDir))
    }

    private void downloadZipArchive(String version, Path zipArchive) {
        URI templates = getURItoDownloadTemplates(version)
        try {
            githubClientConfig
                    .getClient(templates)
                    .getTemplatesZipArchiveForVersion(version)
                    .withCloseable { response ->
                        if (response.status() >= 300) {
                            def methodKey = 'GithubDocumentTemplates..API#getTemplatesZipArchiveForVersion'
                            throw new ErrorDecoder.Default().decode(methodKey, response)
                        }
                        response.body().withCloseable { body ->
                            body.asInputStream().withStream { is ->
                                Files.copy(is, zipArchive, StandardCopyOption.REPLACE_EXISTING)
                            }
                        }
                    }
        } catch (FeignException callException) {
            def baseErrMessage = "Could not get document zip from '${templates}'!- For version:${version}"
            if (callException instanceof FeignException.BadRequest) {
                throw new RuntimeException ("${baseErrMessage} \rIs there a correct release branch configured?")
            } else if (callException instanceof FeignException.NotFound) {
                throw new RuntimeException ("${baseErrMessage}")
            } else {
                throw callException
            }
        }
    }
}
