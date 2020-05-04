package app

import java.nio.file.Path
import org.apache.http.client.utils.URIBuilder
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import feign.Feign
import feign.Headers
import feign.Param
import feign.RequestLine
import util.DocUtils

interface GithubDocumentTemplatesStoreHttpAPI {
  @Headers("Accept: application/octet-stream")
  @RequestLine("GET /opendevstack/ods-document-generation-templates/archive/v{version}.zip")
  byte[] getTemplatesZipArchiveForVersion(@Param("version") String version)
}

class GithubDocumentTemplatesStore implements DocumentTemplatesStore {

    Config config

    // TODO: use dependency injection
    GithubDocumentTemplatesStore() {
        this.config = ConfigFactory.load()
    }

    // Get document templates of a specific version into a target directory
    def Path getTemplatesForVersion(String version, Path targetDir) {
        def uri = getZipArchiveDownloadURI(version)
        Feign.Builder builder = Feign.builder()

        GithubDocumentTemplatesStoreHttpAPI store = builder.target(
            GithubDocumentTemplatesStoreHttpAPI.class,
            uri.getScheme() + "://" + uri.getAuthority()
        )

        def zipArchiveContent = store.getTemplatesZipArchiveForVersion(
            version
        )
        
        return DocUtils.extractZipArchive(
          zipArchiveContent, targetDir, "ods-document-generation-templates-${version}")
    }

    // Get a URI to download document templates of a specific version
    def URI getZipArchiveDownloadURI(String version) {
        // for testing
        String githubUrl = System.getenv("GITHUB_HOST") ?: "https://www.github.com"
        return new URIBuilder(githubUrl)
            .setPath("/opendevstack/ods-document-generation-templates/archive/v${version}.zip")
            .build()
    }
    
    boolean isApplicableToSystemConfig ()
    {
        return true
    }
}
