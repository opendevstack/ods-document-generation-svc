package org.ods.doc.gen;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.restassured.RestAssured.given;

public class LoadSimulation extends Simulation {

    public static final String HEALTH = "http://localhost:8080/health";
    // TODO -> change POST_PDF by "http://localhost:8080/document" and test a big doc
    public static final String POST_PDF = "http://localhost:8080/health";
    public static final int STATUS_CODE_OK = 200;

    public static final int EXECUTION_TIMES = 100;

    // Assertions: https://gatling.io/docs/gatling/reference/current/core/assertions/
    public static final double SUCCESSFUL_REQUEST_PERCENT = 100.0;
    public static final int MAX_RESPONSE_TIME = 400;
    public static final int MEAN_RESPONSE_TIME = 245;

    ScenarioBuilder scn = scenario( "postPDF").repeat(EXECUTION_TIMES).on(
            exec(
                http("POST_PDF")
                    .get(POST_PDF)
                    .asJson()
                    .check(status().is(STATUS_CODE_OK))
            ).pause(1)
    );

    {
        waitUntilDocGenIsUp();
        setUp(scn.injectOpen(atOnceUsers(1))).assertions(
                global().successfulRequests().percent().is(SUCCESSFUL_REQUEST_PERCENT),
                global().responseTime().max().lt(MAX_RESPONSE_TIME),
                global().responseTime().mean().lt(MEAN_RESPONSE_TIME)
        );;
    }

    private void waitUntilDocGenIsUp() {
        Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS).until(() ->
        {
            return given().contentType(ContentType.JSON).when().get(HEALTH).getStatusCode() == STATUS_CODE_OK;
        });
    }
}