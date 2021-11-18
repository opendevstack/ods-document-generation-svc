package org.ods.doc.gen

import io.restassured.http.ContentType
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.web.server.LocalServerPort
import spock.lang.Specification

import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.equalTo

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AppSpec extends Specification {

    @LocalServerPort
    private int port;

    def "Spring App is configured OK"() {
        expect: "health check is OK"
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
