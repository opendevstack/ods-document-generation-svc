package org.ods.doc.gen.pdf.builder.repository

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.ods.doc.gen.core.test.wiremock.WireMockFacade
import org.springframework.stereotype.Repository

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching

@Repository
class WiremockReleaseRepository {

    public static final String RELEASE_MANAGER_REPO_ZIP_PATH = "workspace/"

    private WireMockFacade wireMockFacade

    WiremockReleaseRepository(){
        wireMockFacade = new WireMockFacade()
    }

    def tearDownWiremock(){
        wireMockFacade.stopWireMockServer()
    }

    void setUpDocumentRepository(def project, def releaseManagerRepository, def branchRef) {
        URI templatesURI = GithubDocumentTemplatesRepository.getURItoDownloadTemplates("1.2")
        String uri = WIREMOCK_SERVER_HOST + REPO_ZIP_ARCHIVE_URL
        String matchingUri = BITBUCKET_HOST + REPO_ZIP_ARCHIVE_URL
        String zipFilePath = RELEASE_MANAGER_REPO_ZIP_PATH + "/" + ${project} + "/" + ${releaseManagerRepository} + ".zip"

        mockZipArchiveDownload(uri, matchingUri, zipFilePath)
    }

    private void mockZipArchiveDownload(String uri, String matchingUri, String releaseManagerRepoName, int returnStatus = 200) {
        def zipArchiveContent = getResource(releaseManagerRepoName).readBytes()
        startDocumentsWiremock(uri, matchingUri, zipArchiveContent, returnStatus)
    }

    private StubMapping startDocumentsWiremock(String uri, String matchingUri, byte[] zipArchiveContent, int returnStatus = 200) {
        wireMockFacade.startWireMockServer(new URI(uri)).stubFor(WireMock.get(urlPathMatching(matchingUri))
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
