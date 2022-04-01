package org.ods.doc.gen.external.modules.nexus

import com.github.tomakehurst.wiremock.client.WireMock
import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.test.SpecHelper
import org.ods.doc.gen.core.test.wiremock.WiremockManager
import org.springframework.core.env.Environment
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

@ActiveProfiles(["test"])
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
@Slf4j
class NexusServiceSpec extends SpecHelper {

    static final boolean RECORD = Boolean.parseBoolean(System.properties["testRecordMode"] as String)

    @Inject
    Environment environment

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    NexusService createService(int port, String username, String password) {
        return new NexusService("http://localhost:${port}", username, password)
    }

    def "create with invalid baseURL"() {
        when:
        new NexusService(null, "username", "password")

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "Error: unable to connect to Nexus. 'baseURL' is undefined."

        when:
        new NexusService(" ", "username", "password")

        then:
        e = thrown(IllegalArgumentException)
        e.message == "Error: unable to connect to Nexus. 'baseURL' is undefined."

        when:
        new NexusService("invalid URL", "username", "password")

        then:
        e = thrown(IllegalArgumentException)
        e.message == "Error: unable to connect to Nexus. 'invalid URL' is not a valid URI."
    }

    def "create with invalid username"() {
        when:
        new NexusService("http://localhost", null, "password")

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "Error: unable to connect to Nexus. 'username' is undefined."

        when:
        new NexusService("http://localhost", " ", "password")

        then:
        e = thrown(IllegalArgumentException)
        e.message == "Error: unable to connect to Nexus. 'username' is undefined."
    }

    def "create with invalid password"() {
        when:
        new NexusService("http://localhost", "username", null)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "Error: unable to connect to Nexus. 'password' is undefined."

        when:
        new NexusService("http://localhost", "username", " ")

        then:
        e = thrown(IllegalArgumentException)
        e.message == "Error: unable to connect to Nexus. 'password' is undefined."
    }

    byte[] getExampleFileBytes() {
        return Paths.get("src/test/resources/nexus/LICENSE.zip").toFile().getBytes()
    }

    Map storeArtifactRequestData(Map mixins = [:]) {
        def result = [
            data: [
                artifact: [0] as byte[],
                contentType: "application/octet-stream",
                directory: "myDirectory",
                name: "myName",
                repository: "myRepository",
            ],
            password: "password",
            path: "/service/rest/v1/components",
            username: "username"
        ]

        result.multipartRequestBody = [
            "raw.directory": result.data.directory,
            "raw.asset1": result.data.artifact,
            "raw.asset1.filename": result.data.name
        ]

        result.queryParams = [
            "repository": result.data.repository
        ]

        return result << mixins
    }

    Map getArtifactRequestData(Map mixins = [:]) {
        def result = [
            data: [
                directory: "myDirectory",
                name: "myName",
                repository: "myRepository",
            ],
            password: "password",
            path: "/repository/myRepository/myDirectory/myName",
            username: "username"
        ]
        return result << mixins
  }

    Map storeArtifactResponseData(Map mixins = [:]) {
        def result = [
            status: 204
        ]

        return result << mixins
    }

    def "store artifact (mocked server)"() {
        given:
        def request = storeArtifactRequestData()
        def response = storeArtifactResponseData()

        def server = createServer(WireMock.&post, request, response)
        def service = createService(server.port(), request.username, request.password)

        when:
        def result = service.storeArtifact(request.data.repository, request.data.directory, request.data.name, request.data.artifact, request.data.contentType)

        then:
        result == new URIBuilder("http://localhost:${server.port()}/repository/${request.data.repository}/${request.data.directory}/${request.data.name}").build()

        cleanup:
        stopServer(server)
    }

    def "store artifact (mocked server) with HTTP 404 failure"() {
        given:
        def request = storeArtifactRequestData()
        def response = storeArtifactResponseData([
            status: 404
        ])

        def server = createServer(WireMock.&post, request, response)
        def service = createService(server.port(), request.username, request.password)

        when:
        service.storeArtifact(request.data.repository, request.data.directory, request.data.name, request.data.artifact, request.data.contentType)

        then:
        def e = thrown(RuntimeException)
        e.message.startsWith("Error: unable to store artifact")
        e.message.endsWith("Nexus could not be found at: 'http://localhost:${server.port()}' with repo: ${request.data.repository}.")

        cleanup:
        stopServer(server)
    }

    def "store artifact (mocked server) with HTTP 500 failure"() {
        given:
        def request = storeArtifactRequestData()
        def response = storeArtifactResponseData([
            body: "Sorry, doesn't work!",
            status: 500
        ])

        def server = createServer(WireMock.&post, request, response)
        def service = createService(server.port(), request.username, request.password)

        when:
        service.storeArtifact(request.data.repository, request.data.directory, request.data.name, request.data.artifact, request.data.contentType)

        then:
        def e = thrown(RuntimeException)
        e.message.startsWith("Error: unable to store artifact")
        e.message.endsWith("Nexus responded with code: '${response.status}' and message: 'Sorry, doesn\'t work!'.")

        cleanup:
        stopServer(server)
    }

    def "retrieve artifact (mocked server) with HTTP 404 failure"() {
        given:
        def request = getArtifactRequestData()
        def response = storeArtifactResponseData([
            status: 404
        ])

        def server = createServer(WireMock.&get, request, response)
        def service = createService(server.port(), request.username, request.password)

        when:
        service.retrieveArtifact(request.data.repository, request.data.directory, request.data.name, "abc")

        then:
        def e = thrown(RuntimeException)
        e.message == "Error: unable to get artifact. Nexus could not be found at: 'http://localhost:${server.port()}${request.path}'."

        cleanup:
        stopServer(server)
    }

    def "retrieve artifact (mocked server) working"() {
        given:
        def request = getArtifactRequestData()
        def response = storeArtifactResponseData([
            status: 200
        ])

        def server = createServer(WireMock.&get, request, response)
        def service = createService(server.port(), request.username, request.password)

        when:
        Map result = service.retrieveArtifact(request.data.repository, request.data.directory, request.data.name, "abc")

        then:
        result.uri == new URI("http://localhost:${server.port()}${request.path}")

        cleanup:
        stopServer(server)
    }

    def "Tests downloadAndExtractZip (mocked server)"() {
        given: "An url"
        def request = getArtifactRequestData()
        def response = storeArtifactResponseData([
                status: 200,
                body: getExampleFileBytes(),
        ])

        def server = createServer(WireMock.&get, request, response)
        def service = createService(server.port(), request.username, request.password)
        temporaryFolder.create()

        String url = "/repository/" + request.data.repository + "/" + request.data.directory + "/" + request.data.name

        when: "execute"

        service.downloadAndExtractZip(url, temporaryFolder.getRoot().getAbsolutePath())

        then: "downloads and unzips"
        Paths.get(temporaryFolder.getRoot().getAbsolutePath(), "LICENSE").toFile().exists()
    }

    def "Test upload to nexus with Wiremock" () {
        given:
        String nexusBaseUrl = environment.getProperty("nexus.url")
        String nexusUsername = environment.getProperty("nexus.username")
        String nexusPassword = environment.getProperty("nexus.password")

        log.info "Using RECORD Wiremock:${RECORD}"
        WiremockManager nexusWiremockManager = new WiremockManager("nexus", nexusBaseUrl)
                .withScenario("ordgp/zipUploadTest").startServer(RECORD)
        String nexusMockedBaseURL = nexusWiremockManager.wireMockServer.baseUrl()
        NexusService nexusService = new NexusService(nexusMockedBaseURL, nexusUsername, nexusPassword)

        String repository = NexusService.NEXUS_REPOSITORY
        String directory = "ordgp/69"
        String fileName = "LICENSE-69.zip"
        String contentType = "application/octet-stream"

        Path filePath = Paths.get("src/test/resources/nexus/LICENSE.zip")
        byte [] fileBytes = filePath.toFile().getBytes()

        when:
        URI result = nexusService.storeArtifact(repository, directory, fileName, fileBytes, contentType)

        then:
        result != null
        log.info("Uploaded file url: " + result.toString())

        cleanup:
        nexusWiremockManager.tearDown()
    }

}
