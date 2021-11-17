package app

import groovy.json.JsonOutput

import io.restassured.http.ContentType

import static io.restassured.RestAssured.*
import static org.hamcrest.Matchers.startsWith

class AppSpec extends SpecHelper {

    def setup() {
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT", "myProject")
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_REPO", "myRepo")
        env.set("BITBUCKET_URL", "http://localhost:9001")
        env.set("BITBUCKET_USERNAME", "user")
        env.set("BITBUCKET_PASSWORD", "pass")
    }

    def "POST /document"() {
        expect:
        def version = "1.0"

        mockTemplatesZipArchiveDownload(
            new BitBucketDocumentTemplatesStore()
                .getZipArchiveDownloadURI(version)
        )

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(JsonOutput.toJson([
                metadata: [ type: "InstallationReport", version: version ],
                data: [
                    name: "Project Phoenix",
                    metadata: [
                        header: "header"
                    ]
                ]
            ]))
        .when()
            .port(this.appConfig.getInt("application.port"))
            .post("/document")
        .then()
            .statusCode(200)
            .body("data", startsWith("%PDF-1.4\n".bytes.encodeBase64().toString()))
    }

    def "POST /document without parameter 'metadata.type'"() {
        expect:
        def version = "1.0"

        mockTemplatesZipArchiveDownload(
            new BitBucketDocumentTemplatesStore()
                .getZipArchiveDownloadURI(version)
        )

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(JsonOutput.toJson([
                metadata: [ type: null, version: version ],
                data: [ name: "Project Phoenix" ]
            ]))
        .when()
            .port(this.appConfig.getInt("application.port"))
            .post("/document")
        .then()
            .statusCode(400)
    }

    def "POST /document without parameter 'metadata.version'"() {
        expect:
        def version = "1.0"

        mockTemplatesZipArchiveDownload(
            new BitBucketDocumentTemplatesStore()
                .getZipArchiveDownloadURI(version)
        )

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(JsonOutput.toJson([
                metadata: [ type: "InstallationReport", version: null ],
                data: [ name: "Project Phoenix" ]
            ]))
        .when()
            .port(this.appConfig.getInt("application.port"))
            .post("/document")
        .then()
            .statusCode(400)
    }

    def "POST /document without parameter 'data'"() {
        expect:
        def version = "1.0"

        mockTemplatesZipArchiveDownload(
            new BitBucketDocumentTemplatesStore()
                .getZipArchiveDownloadURI(version)
        )

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(JsonOutput.toJson([
                metadata: [ type: "InstallationReport", version: version ],
                data: null
            ]))
        .when()
            .port(this.appConfig.getInt("application.port"))
            .post("/document")
        .then()
            .statusCode(400)
    }
}
