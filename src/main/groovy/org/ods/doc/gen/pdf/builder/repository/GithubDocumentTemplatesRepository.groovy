package org.ods.doc.gen.pdf.builder.repository

import feign.Feign
import feign.Headers
import feign.Param
import feign.RequestLine
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import org.apache.commons.io.FileUtils
import org.apache.http.client.utils.URIBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Repository

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

interface GithubDocumentTemplatesStoreHttpAPI {
  @Headers("Accept: application/octet-stream")
  @RequestLine("GET /opendevstack/ods-document-generation-templates/archive/v{version}.zip")
  byte[] getTemplatesZipArchiveForVersion(@Param("version") String version)
}

@Slf4j
@Order(1)
@Repository
class GithubDocumentTemplatesRepository implements DocumentTemplatesRepository {

    private ZipFacade zipFacade
    private String basePath

    @Inject
    GithubDocumentTemplatesRepository(ZipFacade zipFacade, @Value('${cache.documents.basePath}') String basePath){
        this.zipFacade = zipFacade
        this.basePath = basePath
    }

    Path getTemplatesForVersion(String version) {
        def targetDir = Paths.get(basePath, version)
        def uri = getURItoDownloadTemplates(version)
        log.info ("Using templates @${uri}")

        GithubDocumentTemplatesStoreHttpAPI store = createStorageClient(uri)
        def zipContent = store.getTemplatesZipArchiveForVersion(version)
        zipFacade.extractZipArchive(zipContent, targetDir)
        moveContentToRootFolder(targetDir, "ods-document-generation-templates-${version}")
        return targetDir
    }

    boolean isApplicableToSystemConfig () {
        return true
    }

    URI getURItoDownloadTemplates(String version) {
        String githubUrl = System.getenv("GITHUB_HOST") ?: "https://www.github.com"
        return new URIBuilder(githubUrl)
                .setPath("/opendevstack/ods-document-generation-templates/archive/v${version}.zip")
                .build()
    }

    private GithubDocumentTemplatesStoreHttpAPI createStorageClient(URI uri) {
        Feign.Builder builder = createFeignBuilder()
        return builder.target(
                GithubDocumentTemplatesStoreHttpAPI.class,
                uri.getScheme() + "://" + uri.getAuthority()
        )
    }

    private Feign.Builder createFeignBuilder() {
        String[] httpProxyHost = System.getenv('HTTP_PROXY')?.trim()?.replace('http://','')?.split(':')
        log.trace ("Proxy setup: ${httpProxyHost ?: 'not found' }")
        if (httpProxyHost && !System.getenv("GITHUB_HOST")) {
            return Feign.builder().client(new feign.okhttp.OkHttpClient(buildHttpClient(httpProxyHost)))
        } else {
            return Feign.builder()
        }
    }

    private OkHttpClient buildHttpClient(String[] httpProxyHost) {
        int httpProxyPort = httpProxyHost.size() == 2 ? Integer.parseInt(httpProxyHost[1]) : 80
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost[0], httpProxyPort))
        return new OkHttpClient().newBuilder().proxy(proxy).build()
    }

    private void moveContentToRootFolder(targetDir, startAtDir){
        FileUtils.copyDirectory(new File(targetDir.toFile(), startAtDir), targetDir.toFile())
        FileUtils.deleteDirectory(new File(targetDir.toFile(), startAtDir))
    }
}
