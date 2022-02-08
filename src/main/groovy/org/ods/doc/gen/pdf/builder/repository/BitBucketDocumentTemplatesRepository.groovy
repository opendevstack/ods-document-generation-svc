package org.ods.doc.gen.pdf.builder.repository

import feign.Feign
import feign.FeignException
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.auth.BasicAuthRequestInterceptor
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.core.ZipFacade
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

@Repository
@FeignClient(name = "bitBucket-client", configuration = BitBucketClientConfig.class)
interface BitBucketDocumentTemplatesStoreHttpAPI {

  @Headers("Accept: application/octet-stream")
  @GetMapping("/rest/api/latest/projects/{documentTemplatesProject}/repos/{documentTemplatesRepo}/archive?at=refs/heads/release/v{version}&format=zip")
  byte[] getTemplatesZipArchiveForVersion(@PathVariable("documentTemplatesProject") String documentTemplatesProject,
                                          @PathVariable("documentTemplatesRepo") String documentTemplatesRepo,
                                          @PathVariable ("version") String version)

}

@Slf4j
@Order(0)
@Repository
class BitBucketDocumentTemplatesRepository implements DocumentTemplatesRepository {

    private final ZipFacade zipFacade
    private final String basePath
    private final BitBucketDocumentTemplatesStoreHttpAPI store

    @Inject
    BitBucketDocumentTemplatesRepository(BitBucketDocumentTemplatesStoreHttpAPI store, ZipFacade zipFacade, @Value('${cache.documents.basePath}') String basePath){
        this.store = store
        this.basePath = basePath
        this.zipFacade = zipFacade
    }

    Path getTemplatesForVersion(String version) {
        log.info ("getTemplatesForVersion version:${version}")

        def targetDir = Paths.get(basePath, version)
        byte[] zipArchiveContent = getZipArchive(version)
        zipFacade.extractZipArchive(zipArchiveContent, targetDir)
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

    URI getURItoDownloadTemplates(String version) {
        def project = System.getenv('BITBUCKET_DOCUMENT_TEMPLATES_PROJECT')
        def repo = System.getenv('BITBUCKET_DOCUMENT_TEMPLATES_REPO')
        return new URIBuilder(System.getenv("BITBUCKET_URL") as String)
                .setPath("/rest/api/latest/projects/${project}/repos/${ repo}/archive")
                .addParameter("at", "refs/heads/release/v${version}")
                .addParameter("format", "zip")
                .build()
    }

    private byte[] getZipArchive(String version) {
        def bitbucketUserName = System.getenv("BITBUCKET_USERNAME")

        def bitbucketRepo = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_REPO")
        def bitbucketProject = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT")
        try {
            return store.getTemplatesZipArchiveForVersion(bitbucketProject, bitbucketRepo, version)
        } catch (FeignException callException) {
            def baseErrMessage = "Could not get document"
            def baseRepoErrMessage = "${baseErrMessage}\rIn repository '${bitbucketRepo}' - "
            if (callException instanceof FeignException.BadRequest) {
                throw new RuntimeException("${baseRepoErrMessage}" +
                        "is there a correct release branch configured, called 'release/v${version}'?")
            } else if (callException instanceof FeignException.Unauthorized) {
                def bbUserNameError = bitbucketUserName ?: 'Anyone'
                throw new RuntimeException("${baseRepoErrMessage} \rDoes '${bbUserNameError}' have access?")
            } else if (callException instanceof FeignException.NotFound) {
                throw new RuntimeException("${baseErrMessage}" +
                        "\rDoes repository '${bitbucketRepo}' in project: '${bitbucketProject}' exist?")
            } else {
                throw callException
            }
        }
    }

    private BitBucketDocumentTemplatesStoreHttpAPI createStorageClient(String bitbucketUserName, String bitbucketPassword, URI uri) {
        Feign.Builder builder = Feign.builder()
        if (bitbucketUserName && bitbucketPassword) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(bitbucketUserName, bitbucketPassword))
        }

        return builder.target(BitBucketDocumentTemplatesStoreHttpAPI.class, uri.getScheme() + "://" + uri.getAuthority())
    }

}
