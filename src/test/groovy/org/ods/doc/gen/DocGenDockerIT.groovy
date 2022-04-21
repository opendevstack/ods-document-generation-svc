package org.ods.doc.gen

import groovy.util.logging.Slf4j
import io.restassured.http.ContentType
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.equalTo

@Slf4j
@Testcontainers
class DocGenDockerIT extends Specification {

    static final String IMAGE_NAME = "ods-document-generation-svc:local"
    static final int SERVER_PORT = 2222
    static final String ROOT__LOG__LEVEL = "ROOT_LOG_LEVEL"
    static final String LOG_LEVEL = "DEBUG"
    static final String SERVER_PORT_NAME = "SERVER_PORT"

    @Shared
    GenericContainer<?> docGenContainer = new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
            .withExposedPorts(SERVER_PORT)
            .withEnv(ROOT__LOG__LEVEL, LOG_LEVEL)
            .withEnv(SERVER_PORT_NAME, SERVER_PORT as String)

    def "docgen is running in docker"() {
        given:
        def port = docGenContainer.firstMappedPort
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log)
        docGenContainer.followOutput(logConsumer)

        expect:
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .when()
                .port(port)
                .get("/health")
                .then()
                .log()
                .all()
                .statusCode(200)
                .body("status", equalTo("passing"))
    }

}
