package org.ods.doc.gen.pdf.builder.repository

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import groovy.util.logging.Slf4j
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.GithubClientConfig
import org.ods.doc.gen.core.test.wiremock.WireMockFacade
import org.springframework.stereotype.Repository

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo

@Slf4j
@Repository
class WiremockDocumentRepository {

    public static final String GH_TEMPLATE = "pdf.builder/ods-document-generation-templates-github-1.2.zip"
    public static final String BB_TEMPLATE = "pdf.builder/ods-document-generation-templates-bitbucket-1.2.zip"

    public static final String BB_URL = "/rest/api/latest/projects/myProject/repos/myRepo/archive"
    public static final String BB_PROJECT = "myProject"
    public static final String BB_REPO = "myRepo"

    public static final String GH_URL = "/opendevstack/ods-document-generation-templates/archive/vVERSION_ID.zip"
    public static final String VERSION = "VERSION_ID"

    public static final int RETURN_OK = 200
    public static final String APP = "application/octet-stream"
    public static final String ACCEPT = "Accept"
    public static final String ZIP = "zip"

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
        String githubUrl = mockTemplatesZipArchiveDownload(GH_URL.replace(VERSION, version), GH_TEMPLATE)
        log.info("WireMockServer: [github: ${githubUrl}]")
        githubClientConfig.setUrl(githubUrl)
        githubClientConfig.setHttpProxyHost(null)
        bitBucketDocumentTemplatesRepository.bbDocProject = BitBucketDocumentTemplatesRepository.NONE
    }

    void setUpBitbucketRepository(String version) {
        Map queryParams = [:]
        queryParams.at = equalTo("${BitBucketDocumentTemplatesRepository.BRANCH}${version}")
        queryParams.format = equalTo(ZIP)

        String serverUrl = mockTemplatesZipArchiveDownload(BB_URL, BB_TEMPLATE, RETURN_OK, queryParams)
        bitBucketClientConfig.setUrl(serverUrl)
        bitBucketDocumentTemplatesRepository.bbDocProject = BB_PROJECT
        bitBucketDocumentTemplatesRepository.bbRepo = BB_REPO
    }

    private String mockTemplatesZipArchiveDownload(String urlPartialPath,
                                                   String templatesName,
                                                   int returnStatus = RETURN_OK,
                                                   Map queryParams = null) {
        def zipArchiveContent = getResource(templatesName).readBytes()
        return startDocumentsWiremock(urlPartialPath, zipArchiveContent, returnStatus, queryParams)
    }

    private String startDocumentsWiremock(
            String urlPartialPath,
            byte[] zipArchiveContent,
            int returnStatus = RETURN_OK,
            Map queryParams = null) {
        MappingBuilder mappingBuilder = stubMap(urlPartialPath, queryParams, zipArchiveContent, returnStatus)
        wireMockFacade.startWireMockServer().stubFor(mappingBuilder)
        return wireMockFacade.wireMockServer.baseUrl()
    }

    private MappingBuilder stubMap(String urlPartialPath, Map queryParams, byte[] zipArchiveContent, int returnStatus) {
        MappingBuilder maps
        maps = WireMock.get(WireMock.urlPathEqualTo(urlPartialPath)).withHeader(ACCEPT, WireMock.equalTo(APP))
        if (queryParams)
            maps.withQueryParams(queryParams)
        maps.willReturn(WireMock.aResponse().withBody(zipArchiveContent).withStatus(returnStatus))
        return maps
    }

    private File getResource(String name) {
        new File(getClass().getClassLoader().getResource(name).getFile())
    }

}
