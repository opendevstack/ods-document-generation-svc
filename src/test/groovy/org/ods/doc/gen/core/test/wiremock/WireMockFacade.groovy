package org.ods.doc.gen.core.test.wiremock

import com.github.tomakehurst.wiremock.WireMockServer

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

class WireMockFacade {

    WireMockServer wireMockServer

    def cleanup() {
        stopWireMockServer()
    }

    WireMockServer startWireMockServer(URI uri) {
        this.wireMockServer = new WireMockServer(options().port(uri.getPort()))
        this.wireMockServer.start()
        return this.wireMockServer
    }

    void stopWireMockServer() {
        if (this.wireMockServer != null) {
            this.wireMockServer.stop()
            this.wireMockServer = null
        }
    }

}