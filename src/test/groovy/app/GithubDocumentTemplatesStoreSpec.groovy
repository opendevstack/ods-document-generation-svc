package app

import com.github.tomakehurst.wiremock.client.WireMock

import java.nio.file.Files
import java.nio.file.Paths

import static com.github.tomakehurst.wiremock.client.WireMock.*

class GithubDocumentTemplatesStoreSpec extends SpecHelper {

    def "getTemplatesForVersion"() {
        given:
        def store = new GithubDocumentTemplatesStore()
        def targetDir = Files.createTempDirectory("doc-gen-templates-")
        def version = "1.0"

//        mockTemplatesZipArchiveDownload(store.getZipArchiveDownloadURI(version))

        when:
        def path = store.getTemplatesForVersion(version, targetDir)

        then:
        Paths.get(path.toString(), "templates").toFile().exists()
        Paths.get(path.toString(), "templates", "footer.inc.html.tmpl").toFile().exists()
        Paths.get(path.toString(), "templates", "header.inc.html.tmpl").toFile().exists()
        Paths.get(path.toString(), "templates", "InstallationReport.html.tmpl").toFile().exists()

        cleanup:
        targetDir.toFile().deleteDir()
    }
}
