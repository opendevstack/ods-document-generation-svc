package org.ods.doc.gen.pdf.builder.repository

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.ods.doc.gen.core.test.wiremock.WireMockFacade
import org.springframework.stereotype.Repository
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

import static com.github.tomakehurst.wiremock.client.WireMock.*

@Repository
class WiremockDocumentRepository {

    public static final String GH_TEMPLATE = "pdf.builder/ods-document-generation-templates-github-1.2.zip"
    public static final String BB_TEMPLATE = "pdf.builder/ods-document-generation-templates-bitbucket-1.2.zip"

    public static final String WIREMOCK_SERVER = "http://localhost:9002"

    private WireMockFacade wireMockFacade

    EnvironmentVariables env

    WiremockDocumentRepository(){
        env = new EnvironmentVariables()
        wireMockFacade = new WireMockFacade()
    }

    def tearDownWiremock(){
        env.teardown()
        wireMockFacade.stopWireMockServer()
    }

    void setUpGithubRepository(String version) {
        setupGitHubEnv()
        String url = "/opendevstack/ods-document-generation-templates/archive/v${version}.zip"
        mockTemplatesZipArchiveDownload(url, GH_TEMPLATE)
    }

    void setUpBitbucketRepository(String version) {
        setupBitBuckectEnv()
        Map queryParams = [:]
        queryParams.at = equalTo("refs/heads/release/v1.2")
        queryParams.format = equalTo("zip")
        mockTemplatesZipArchiveDownload(
                "/rest/api/latest/projects/myProject/repos/myRepo/archive",
                BB_TEMPLATE,
                200,
                queryParams
        )
    }

    private setupBitBuckectEnv() {
        env.setup()
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT", "myProject")
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_REPO", "myRepo")
        env.set("BITBUCKET_URL", WIREMOCK_SERVER)
    }

    private setupGitHubEnv() {
        env.setup()
        env.set("GITHUB_HOST", WIREMOCK_SERVER)
    }

    private void mockTemplatesZipArchiveDownload(String urlPartialPath,
                                                 String templatesName,
                                                 int returnStatus = 200,
                                                 Map queryParams = null) {
        def zipArchiveContent = getResource(templatesName).readBytes()
        startDocumentsWiremock(urlPartialPath, zipArchiveContent, returnStatus, queryParams)
    }

    private StubMapping startDocumentsWiremock(
            String urlPartialPath,
            byte[] zipArchiveContent,
            int returnStatus = 200,
            Map queryParams = null) {
        wireMockFacade
                .startWireMockServer(URI.create(WIREMOCK_SERVER))
                .stubFor(stubMaps(urlPartialPath, queryParams, zipArchiveContent, returnStatus))
    }

    private MappingBuilder stubMaps(String urlPartialPath, Map queryParams, byte[] zipArchiveContent, int returnStatus) {
        MappingBuilder maps = WireMock.get(WireMock.urlPathEqualTo(urlPartialPath))
                .withHeader("Accept", WireMock.equalTo("application/octet-stream"))
        if (queryParams)
            maps.withQueryParams(queryParams)
        maps.willReturn(WireMock.aResponse().withBody(zipArchiveContent).withStatus(returnStatus))
        return maps
    }

    private File getResource(String name) {
        new File(getClass().getClassLoader().getResource(name).getFile())
    }

}
