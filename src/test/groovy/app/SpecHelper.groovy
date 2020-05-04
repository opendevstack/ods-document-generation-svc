package app

import com.github.tomakehurst.wiremock.*
import com.github.tomakehurst.wiremock.client.*
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import java.net.URI

import org.jooby.Jooby
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables

import spock.lang.*

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

class SpecHelper extends Specification {
    @Shared App app
    @Shared Config appConfig
    @Shared WireMockServer wireMockServer

    @Rule
    EnvironmentVariables env = new EnvironmentVariables()

    def setupSpec() {
        this.app = new App()
        this.app.start("server.join=false")

        this.appConfig = ConfigFactory.load()
    }

    def cleanupSpec() {
        this.app.stop()
    }

    def cleanup() {
        stopWireMockServer()
    }

    // Get a test resource file by name
    def File getResource(String name) {
        new File(getClass().getClassLoader().getResource(name).getFile())
    }

    // Starts a WireMock server to serve the templates.zip test resource at URI
    def mockTemplatesZipArchiveDownload(URI uri) {
        def zipArchiveContent = getResource("templates.zip").readBytes()

        // Configure and start WireMock server to serve the Zip archive content at URI
        startWireMockServer(uri).stubFor(WireMock.get(urlPathMatching(uri.getPath()))
            .withHeader("Accept", equalTo("application/octet-stream"))
            .willReturn(aResponse()
                .withBody(zipArchiveContent)
                .withStatus(200)
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