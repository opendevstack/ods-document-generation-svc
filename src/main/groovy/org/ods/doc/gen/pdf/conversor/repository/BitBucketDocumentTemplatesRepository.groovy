package org.ods.doc.gen.pdf.conversor.repository

import feign.Feign
import feign.FeignException
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.auth.BasicAuthRequestInterceptor
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.stereotype.Repository

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

interface BitBucketDocumentTemplatesStoreHttpAPI {
  @Headers("Accept: application/octet-stream")
  @RequestLine("GET /rest/api/latest/projects/{documentTemplatesProject}/repos/{documentTemplatesRepo}/archive?at=refs/heads/release/v{version}&format=zip")
  byte[] getTemplatesZipArchiveForVersion(@Param("documentTemplatesProject") String documentTemplatesProject, @Param("documentTemplatesRepo") String documentTemplatesRepo, @Param("version") String version)
}

@Slf4j
@Order(0)
@Repository
class BitBucketDocumentTemplatesRepository implements DocumentTemplatesRepository {

    private ZipFacade zipFacade
    private String basePath

    @Inject
    BitBucketDocumentTemplatesRepository(ZipFacade zipFacade, @Value('${documents.cache.basePath}') String basePath){
        this.basePath = basePath
        this.zipFacade = zipFacade
    }

    Path getTemplatesForVersion(String version) {
        def targetDir = Paths.get(basePath, version)
        URI uri = getURItoDownloadTemplates(version)
        def bitbucketUserName = System.getenv("BITBUCKET_USERNAME")
        def bitbucketPassword = System.getenv("BITBUCKET_PASSWORD")
        log.info ("Using templates @${uri}")

        BitBucketDocumentTemplatesStoreHttpAPI store = createStorageClient(bitbucketUserName, bitbucketPassword, uri)
        byte[] zipArchiveContent = getZipArchive(store, version, uri, bitbucketUserName)
        return zipFacade.extractZipArchive(zipArchiveContent, targetDir)
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

    private byte[] getZipArchive(BitBucketDocumentTemplatesStoreHttpAPI store, String version, uri, String bitbucketUserName) {
        def bitbucketRepo = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_REPO")
        def bitbucketProject = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT")
        try {
            return store.getTemplatesZipArchiveForVersion(bitbucketProject, bitbucketRepo, version)
        } catch (FeignException callException) {
            def baseErrMessage = "Could not get document zip from '${uri}'!"
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
