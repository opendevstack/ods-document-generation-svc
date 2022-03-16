package org.ods.doc.gen.pdf.builder.repository

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.GithubClientConfig
import org.ods.doc.gen.core.test.wiremock.WireMockFacade
import org.springframework.stereotype.Repository

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo

@Repository
class WiremockDocumentRepository {

    public static final String GH_TEMPLATE = "pdf.builder/ods-document-generation-templates-github-1.2.zip"
    public static final String BB_TEMPLATE = "pdf.builder/ods-document-generation-templates-bitbucket-1.2.zip"

    private WireMockFacade wireMockFacade

    private final BitBucketClientConfig bitBucketClientConfig
    private final GithubClientConfig githubClientConfig
    private final BitBucketDocumentTemplatesRepository bitBucketDocumentTemplatesRepository

    WiremockDocumentRepository(BitBucketClientConfig bitBucketClientConfig,
                               GithubClientConfig githubClientConfig,
                               BitBucketDocumentTemplatesRepository bitBucketDocumentTemplatesRepository){
        this.githubClientConfig = githubClientConfig
        this.bitBucketClientConfig = bitBucketClientConfig
        this.bitBucketDocumentTemplatesRepository = bitBucketDocumentTemplatesRepository
        wireMockFacade = new WireMockFacade()
    }

    def tearDownWiremock(){
        wireMockFacade.stopWireMockServer()
    }

    void setUpGithubRepository(String version) {
        String urlPath = "/opendevstack/ods-document-generation-templates/archive/v${version}.zip"
        mockTemplatesZipArchiveDownload(urlPath, GH_TEMPLATE)
    }

    void setUpBitbucketRepository(String version) {
        Map queryParams = [:]
        queryParams.at = equalTo("refs/heads/release/v${version}")
        queryParams.format = equalTo("zip")
        String urlPath = "/rest/api/latest/projects/opendevstack/repos/ods-document-generation-templates/archive"
        mockTemplatesZipArchiveDownload(urlPath, BB_TEMPLATE, 200, queryParams)
    }

    private String mockTemplatesZipArchiveDownload(String urlPartialPath,
                                                 String templatesName,
                                                 int returnStatus = 200,
                                                 Map queryParams = null) {
        def zipArchiveContent = getResource(templatesName).readBytes()
        return startDocumentsWiremock(urlPartialPath, zipArchiveContent, returnStatus, queryParams)
    }

    private String startDocumentsWiremock(
            String urlPartialPath,
            byte[] zipArchiveContent,
            int returnStatus = 200,
            Map queryParams = null) {
        wireMockFacade.startWireMockServer()
                .stubFor(stubMaps(urlPartialPath, queryParams, zipArchiveContent, returnStatus))
        return wireMockFacade.wireMockServer.baseUrl()
    }

    private MappingBuilder stubMaps(String urlPartialPath, Map queryParams, byte[] zipArchiveContent, int returnStatus) {
        MappingBuilder maps = WireMock
                .get(WireMock.urlPathEqualTo(urlPartialPath))
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
