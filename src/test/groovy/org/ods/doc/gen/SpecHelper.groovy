package org.ods.doc.gen


import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import spock.lang.Shared
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

class SpecHelper extends Specification {

    @Shared WireMockServer wireMockServer



    def cleanup() {
        stopWireMockServer()
    }

    // Get a test resource file by name
    def File getResource(String name) {
        new File(getClass().getClassLoader().getResource(name).getFile())
    }

    // Starts a WireMock server to serve the templates.zip test resource at URI
    def mockTemplatesZipArchiveDownload(URI uri, int returnStatus = 200) {
        def zipArchiveContent = getResource("templates.zip").readBytes()

        // Configure and start WireMock server to serve the Zip archive content at URI
        startWireMockServer(uri).stubFor(WireMock.get(urlPathMatching(uri.getPath()))
            .withHeader("Accept", equalTo("application/octet-stream"))
            .willReturn(aResponse()
                .withBody(zipArchiveContent)
                .withStatus(returnStatus)
            ))
    }

    // Starts a WireMock server to serve the github templates.zip test resource at URI
    def mockGithubTemplatesZipArchiveDownload(URI uri) {
        def zipArchiveContent = getResource("github-ods-document-generation-templates-1.0.zip").readBytes()

        // Configure and start WireMock server to serve the Zip archive content at URI
        startWireMockServer(uri).stubFor(WireMock.get(urlPathMatching(uri.getPath()))
            .withHeader("Accept", equalTo("application/octet-stream"))
            .willReturn(aResponse()
                .withBody(zipArchiveContent)
                .withStatus(200)
            ))
    }

    // Starts and configures a WireMock server
    def WireMockServer startWireMockServer(URI uri) {
        this.wireMockServer = new WireMockServer(options()
            .port(uri.getPort())
        )

        this.wireMockServer.start()
        WireMock.configureFor(uri.getPort())

        return this.wireMockServer
    }

    // Stops a WireMock server
    def void stopWireMockServer() {
        if (this.wireMockServer != null) {
            this.wireMockServer.stop()
            this.wireMockServer = null
        }
    }
}