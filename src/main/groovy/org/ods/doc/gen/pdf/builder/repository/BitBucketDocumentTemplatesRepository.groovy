package org.ods.doc.gen.pdf.builder.repository


import feign.FeignException
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.Response
import feign.codec.ErrorDecoder
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.core.ZipFacade
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Repository

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

interface BitBucketDocumentTemplatesStoreHttpAPI {

  @Headers("Accept: application/octet-stream")
  @RequestLine("GET /rest/api/latest/projects/{documentTemplatesProject}/repos/{documentTemplatesRepo}/archive?at=refs/heads/release/v{version}&format=zip")
  Response getTemplatesZipArchiveForVersion(@Param("documentTemplatesProject") String documentTemplatesProject,
                                            @Param("documentTemplatesRepo") String documentTemplatesRepo,
                                            @Param("version") String version)

}

@Slf4j
@Order(0)
@Repository
class BitBucketDocumentTemplatesRepository implements DocumentTemplatesRepository {

    private final ZipFacade zipFacade
    private final String basePath
    private final BitBucketClientConfig bitBucketClientConfig

    @Inject
    BitBucketDocumentTemplatesRepository(BitBucketClientConfig bitBucketClientConfig,
                                         ZipFacade zipFacade,
                                         @Value('${cache.documents.basePath}') String basePath){
        this.bitBucketClientConfig = bitBucketClientConfig
        this.basePath = basePath
        this.zipFacade = zipFacade
    }

    Path getTemplatesForVersion(String version) {
        log.info ("getTemplatesForVersion version:${version}")

        Path targetDir = Paths.get(basePath, version)
        Path zipArchive = Files.createTempFile("archive-", ".zip")
        try {
            downloadZipArchive(version, zipArchive)
            zipFacade.extractZipArchive(zipArchive, targetDir)
        } catch (Throwable e) {
            throw e
        } finally {
            Files.delete(zipArchive)
        }

        return targetDir
    }

    boolean isApplicableToSystemConfig () {
        List missingEnvs = [ ]
        if (!System.getenv("BITBUCKET_URL")) {
            missingEnvs << "BITBUCKET_URL"
        }
        if (!System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT")) {
            missingEnvs << "BITBUCKET_DOCUMENT_TEMPLATES_PROJECT"
        }
        if (!System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_REPO")) {
            missingEnvs << "BITBUCKET_DOCUMENT_TEMPLATES_REPO"
        }
        if (missingEnvs.size() > 0) {
            log.warn "Bitbucket adapter not applicable - missing config '${missingEnvs}'"
            return false
        }

        return true
    }

    static URI getURItoDownloadTemplates(String version) {
        def project = System.getenv('BITBUCKET_DOCUMENT_TEMPLATES_PROJECT')
        def repo = System.getenv('BITBUCKET_DOCUMENT_TEMPLATES_REPO')
        return new URIBuilder(System.getenv("BITBUCKET_URL") as String)
                .setPath("/rest/api/latest/projects/${project}/repos/${ repo}/archive")
                .addParameter("at", "refs/heads/release/v${version}")
                .addParameter("format", "zip")
                .build()
    }

    private void downloadZipArchive(String version, Path zipArchive) {
        def bitbucketRepo = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_REPO")
        def bitbucketProject = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT")
        URI templates = getURItoDownloadTemplates(version)
        try {
            bitBucketClientConfig
                .getClient(templates)
                .getTemplatesZipArchiveForVersion(bitbucketProject, bitbucketRepo, version)
                .withCloseable { response ->
                    if (response.status() >= 300) {
                        def methodKey = 'BitBucketDocumentTempla..API#getTemplatesZipArchiveForVersion'
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
            } else if (callException instanceof FeignException.Unauthorized) {
                def bbUserNameError =  System.getenv("BITBUCKET_USERNAME") ?: 'Anyone'
                throw new RuntimeException ("${baseErrMessage} \rDoes '${bbUserNameError}' have access?")
            } else if (callException instanceof FeignException.NotFound) {
                throw new RuntimeException ("${baseErrMessage}")
            } else {
                throw callException
            }
        }
    }

}
