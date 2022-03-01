package org.ods.doc.gen.pdf.builder.repository

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.ods.doc.gen.core.test.wiremock.WireMockFacade
import org.springframework.stereotype.Repository
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching

@Repository
class WiremockDocumentRepository {

    public static final String GH_TEMPLATE = "pdf.builder/ods-document-generation-templates-github-1.2.zip"
    public static final String BB_TEMPLATE = "pdf.builder/ods-document-generation-templates-bitbucket-1.2.zip"

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
        mockTemplatesZipArchiveDownload(GithubDocumentTemplatesRepository.getURItoDownloadTemplates(version), GH_TEMPLATE)
    }

    void setUpBitbucketRepository(String version) {
        setupBitBuckectEnv()
        mockTemplatesZipArchiveDownload(BitBucketDocumentTemplatesRepository.getURItoDownloadTemplates(version), BB_TEMPLATE)
    }

    private setupBitBuckectEnv() {
        env.setup()
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT", "myProject")
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_REPO", "myRepo")
        env.set("BITBUCKET_URL", "http://localhost:9002")
    }

    private setupGitHubEnv() {
        env.setup()
        env.set("GITHUB_HOST", "http://localhost:9002")
    }

    private void mockTemplatesZipArchiveDownload(URI uri, String templatesName, int returnStatus = 200) {
        def zipArchiveContent = getResource(templatesName).readBytes()
        startDocumentsWiremock(uri, zipArchiveContent, returnStatus)
    }

    private StubMapping startDocumentsWiremock(URI uri, byte[] zipArchiveContent, int returnStatus = 200) {
        wireMockFacade.startWireMockServer(uri).stubFor(WireMock.get(urlPathMatching(uri.getPath()))
                .withHeader("Accept", equalTo("application/octet-stream"))
                .willReturn(aResponse()
                        .withBody(zipArchiveContent)
                        .withStatus(returnStatus)
                ))
    }

    private File getResource(String name) {
        new File(getClass().getClassLoader().getResource(name).getFile())
    }

}
