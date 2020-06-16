package app

import util.DocUtils
import com.google.inject.Inject
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import feign.Feign
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.auth.BasicAuthRequestInterceptor;
import feign.FeignException

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

import net.lingala.zip4j.core.ZipFile

import org.apache.http.client.utils.URIBuilder

interface BitBucketDocumentTemplatesStoreHttpAPI {
  @Headers("Accept: application/octet-stream")
  @RequestLine("GET /rest/api/latest/projects/{documentTemplatesProject}/repos/{documentTemplatesRepo}/archive?at=refs/heads/release/v{version}&format=zip")
  byte[] getTemplatesZipArchiveForVersion(@Param("documentTemplatesProject") String documentTemplatesProject, @Param("documentTemplatesRepo") String documentTemplatesRepo, @Param("version") String version)
}

class BitBucketDocumentTemplatesStore implements DocumentTemplatesStore {

    Config config

    // TODO: use dependency injection
    BitBucketDocumentTemplatesStore() {
        this.config = ConfigFactory.load()
    }

    // Get document templates of a specific version into a target directory
    def Path getTemplatesForVersion(String version, Path targetDir) {
        def uri = getZipArchiveDownloadURI(version)

        Feign.Builder builder = Feign.builder()

        def bitbucketUserName = System.getenv("BITBUCKET_USERNAME")
        def bitbucketPassword = System.getenv("BITBUCKET_PASSWORD")
        if (bitbucketUserName && bitbucketPassword) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(
                bitbucketUserName, bitbucketPassword
            ))
        }

        BitBucketDocumentTemplatesStoreHttpAPI store = builder.target(
            BitBucketDocumentTemplatesStoreHttpAPI.class,
            uri.getScheme() + "://" + uri.getAuthority()
        )

        def bitbucketRepo = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_REPO")
        def bitbucketProject = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT")
        def zipArchiveContent
        try {
            zipArchiveContent = store.getTemplatesZipArchiveForVersion(
                bitbucketProject,
                bitbucketRepo,
                version
            )
        } catch (FeignException callException) {
            def baseErrMessage = "Could not get document zip from '${uri}'!"
            def baseRepoErrMessage = "${baseErrMessage}\rIn repository '${bitbucketRepo}' - "
            if (callException instanceof FeignException.BadRequest) {
                throw new RuntimeException ("${baseRepoErrMessage}" +
                    "is there a correct release branch configured, called 'release/v${version}'?")
            } else if (callException instanceof FeignException.Unauthorized) {
                def bbUserNameError = bitbucketUserName ?: 'Anyone'
                throw new RuntimeException ("${baseRepoErrMessage}" +
                    "does '${bbUserNameError}' have access?")
            } else if (callException instanceof FeignException.NotFound) {
                throw new RuntimeException ("${baseErrMessage}" +
                    "\rDoes repository '${bitbucketRepo}' in project: '${bitbucketProject}' exist?")
            } else {
                throw callException
            }
        }
        return DocUtils.extractZipArchive(zipArchiveContent, targetDir)
    }

    // Get a URI to download document templates of a specific version
    def URI getZipArchiveDownloadURI(String version) {
        return new URIBuilder(System.getenv("BITBUCKET_URL"))
            .setPath("/rest/api/latest/projects/${System.getenv('BITBUCKET_DOCUMENT_TEMPLATES_PROJECT')}/repos/${System.getenv('BITBUCKET_DOCUMENT_TEMPLATES_REPO')}/archive")
            .addParameter("at", "refs/heads/release/v${version}")
            .addParameter("format", "zip")
            .build()
    }

    boolean isApplicableToSystemConfig () 
    {
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
          println "[ERROR]: Bitbucket adapter not applicable - missing config '${missingEnvs}'"
          return false
        }
        
        return true
    }
}
