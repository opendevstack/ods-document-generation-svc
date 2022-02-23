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

    public static final String RELEASE_MANAGER_REPO_CONTENT_ZIP = "pdf.builder/release-manager-repo-content.zip"

    private WireMockFacade wireMockFacade

    WiremockReleaseRepository(){
        wireMockFacade = new WireMockFacade()
    }

    def tearDownWiremock(){
        wireMockFacade.stopWireMockServer()
    }

    void setUpBitbucketRepository(def documentTemplatesProject, def documentTemplatesRepo, def version) {
        def uri = new URI("http://localhost:9003/rest/api/latest/projects/${documentTemplatesProject}/repos/${documentTemplatesRepo}/archive?at=${version}&format=zip")
        mockZipArchiveDownload(uri, RELEASE_MANAGER_REPO_CONTENT_ZIP)
    }

    private void mockZipArchiveDownload(URI uri, String releaseManagerRepoName, int returnStatus = 200) {
        def zipArchiveContent = getResource(releaseManagerRepoName).readBytes()
        startDocumentsWiremock(uri, zipArchiveContent, returnStatus)
    }

    private StubMapping startDocumentsWiremock(URI uri, byte[] zipArchiveContent, int returnStatus = 200) {
        def matchingUri = "https://bitbucket-dev.biscrum.com/rest/api/latest/projects/ordgp/repos/ordgp-releasemanager.git/archive?at=refs/heads/master&format=zip"
        wireMockFacade.startWireMockServer(uri).stubFor(WireMock.get(urlPathMatching(matchingUri))
//                .withHeader("Accept", equalTo("application/octet-stream"))
                .willReturn(aResponse()
                        .withBody(zipArchiveContent)
                        .withStatus(returnStatus)
                ))
    }

    private File getResource(String name) {
        new File(getClass().getClassLoader().getResource(name).getFile())
    }
}
