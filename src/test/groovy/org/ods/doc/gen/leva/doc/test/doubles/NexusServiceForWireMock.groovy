package org.ods.doc.gen.leva.doc.test.doubles

import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.adapters.nexus.NexusService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

import javax.inject.Inject

@Profile("test")
@Repository
class NexusServiceForWireMock extends NexusService {

    @Inject
    NexusServiceForWireMock(@Value('${nexus.url}') String baseURL,
                            @Value('${nexus.username}') String username,
                            @Value('${nexus.password}') String password) {
        super(baseURL, username, password)
    }

    @Override
    URI storeArtifact(String directory, String name, String artifactPath, String contentType)  {
        return new URIBuilder("${baseURL}/repository/${directory}/${name}").build()
    }

}
