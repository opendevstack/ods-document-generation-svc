package app

import feign.Response
import feign.codec.ErrorDecoder
import okhttp3.OkHttpClient
import org.apache.http.client.utils.URIBuilder
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import feign.Feign
import feign.Headers
import feign.Param
import feign.RequestLine
import util.DocUtils
import util.FileTools

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

interface GithubDocumentTemplatesStoreHttpAPI {
  @Headers("Accept: application/octet-stream")
  @RequestLine("GET /opendevstack/ods-document-generation-templates/archive/v{version}.zip")
  Response getTemplatesZipArchiveForVersion(@Param("version") String version)
}

class GithubDocumentTemplatesStore implements DocumentTemplatesStore {

    Config config

    // TODO: use dependency injection
    GithubDocumentTemplatesStore() {
        this.config = ConfigFactory.load()
    }

    // Get document templates of a specific version into a target directory
    Path getTemplatesForVersion(String version, Path targetDir) {
        def uri = getZipArchiveDownloadURI(version)
        Feign.Builder builder = createBuilder()['builder']

        GithubDocumentTemplatesStoreHttpAPI store = builder.target(
            GithubDocumentTemplatesStoreHttpAPI.class,
            uri.getScheme() + "://" + uri.getAuthority()
        )

        return store.getTemplatesZipArchiveForVersion(version).withCloseable { response ->
            if (response.status() >= 300) {
                def methodKey =
                        'GithubDocumentTemplatesStoreHttpAPI#getTemplatesZipArchiveForVersion(String)'
                throw new ErrorDecoder.Default().decode(methodKey, response)
            }
            return FileTools.withTempFile('tmpl', 'zip') { zipArchive ->
                response.body().withCloseable { body ->
                    body.asInputStream().withStream { is ->
                        Files.copy(is, zipArchive, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
                return DocUtils.extractZipArchive(
                        zipArchive, targetDir, "ods-document-generation-templates-${version}")
            }
        }
    }

    // Get a URI to download document templates of a specific version
    URI getZipArchiveDownloadURI(String version) {
        // for testing
        String githubUrl = System.getenv("GITHUB_HOST") ?: "https://www.github.com"
        return new URIBuilder(githubUrl)
            .setPath("/opendevstack/ods-document-generation-templates/archive/v${version}.zip")
            .build()
    }

    // proxy setup, we return a map for testing
    Map createBuilder () {
        String[] httpProxyHost = System.getenv('HTTP_PROXY')?.trim()?.replace('http://','')?.split(':')
        println ("Proxy setup: ${httpProxyHost ?: 'not found' }")
        if (httpProxyHost && !System.getenv("GITHUB_HOST")) {
            int httpProxyPort = httpProxyHost.size() == 2 ? Integer.parseInt(httpProxyHost[1]) : 80
            Proxy proxy = new Proxy(Proxy.Type.HTTP, 
                new InetSocketAddress(httpProxyHost[0], httpProxyPort))
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder().proxy(proxy).build()
            return [ 
                'builder': Feign.builder().client(new feign.okhttp.OkHttpClient(okHttpClient)),
                'proxy' : proxy
            ]
        } else {
            return ['builder' : Feign.builder()]
        }
    }

    boolean isApplicableToSystemConfig ()
    {
        return true
    }
}
