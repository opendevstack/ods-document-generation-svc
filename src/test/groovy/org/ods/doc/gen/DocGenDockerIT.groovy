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

    static final DockerImageName DOCGEN_IMAGE = DockerImageName.parse("ods-document-generation-svc:local");

    @Shared
    GenericContainer<?> docGenContainer = new GenericContainer<>(DOCGEN_IMAGE)
            .withExposedPorts(8080)
            .withEnv("ROOT_LOG_LEVEL", "TRACE")
            .withEnv("SERVER_PORT", "8080")

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
