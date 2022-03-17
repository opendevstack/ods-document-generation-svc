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

/**
 * Run it with DockerTest action, you should have a Docker client
 */
@Slf4j
@Testcontainers
class DocGenDockerIT extends Specification {

    public static final String IMAGE_NAME = "ods-document-generation-svc:local"
    static final DockerImageName DOCGEN_IMAGE = DockerImageName.parse(IMAGE_NAME)
    static final int SERVER_PORT = 1111
    static final String SERVER_PORT_CHANGED_WITH_ENV_VAR = "8080"
    static final String ROOT__LOG__LEVEL = "ROOT_LOG_LEVEL"
    static final String TRACE = "TRACE"
    static final String SERVER_PORT_NAME = "SERVER_PORT"

    @Shared
    GenericContainer<?> docGenContainer = new GenericContainer<>(DOCGEN_IMAGE)
            .withExposedPorts(SERVER_PORT)
            .withEnv(ROOT__LOG__LEVEL, TRACE)
            .withEnv(SERVER_PORT_NAME, SERVER_PORT_CHANGED_WITH_ENV_VAR)

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
