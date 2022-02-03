package org.ods.doc.gen.leva.doc.api

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocDataFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.ProjectFixture
import org.ods.doc.gen.leva.doc.services.LeVADocumentService
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

import static org.mockito.ArgumentMatchers.anyMap
import static org.mockito.ArgumentMatchers.argThat
import static org.mockito.Mockito.when
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Slf4j
@WebMvcTest(LevaDocController.class)
class LevaDocControllerSpec extends Specification {

    @TempDir
    public Path tempFolder

    @Inject
    private MockMvc mockMvc

    @MockBean
    LeVADocumentService leVADocumentService

    LevaDocDataFixture dataFixture

    def setup() {
        String simpleName = this.class.simpleName
        dataFixture = new LevaDocDataFixture(simpleName, tempFolder.toFile())
    }

    def "BuildDocument ok"() {
        given: "A temporal folder "
        GroovySpy(Files, global: true)
        Files.createTempDirectory(_) >> tempFolder

        and: "leVADocumentService is mocked"
        def urlDocType = "URL nexus artifact"
        Map data = dataFixture.buildFixtureData(projectFixture)
        Map serviceParam = buildServiceDataParam(projectFixture, data)
        when(leVADocumentService.createCSD(argThat(map -> map == serviceParam))).thenReturn(urlDocType)

        expect: "a client call to /levaDoc/TEST_PROJECT_ID/2/CSD return the url of the doc created"
        this.mockMvc
                .perform(post("/levaDoc/TEST_PROJECT_ID/2/CSD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonOutput.toJson(data)))
                .andExpect(status().isOk()).andExpect(content().string(urlDocType))

        and: "the tmp folder is deleted"
        !Files.exists(tempFolder)

        where: "use valid data to generate pdf"
        projectFixture =  ProjectFixture.getProjectFixtureBuilder(getProject(), "CSD").build()
    }


    def "BuildDocument when LevaDoc Throw exception"() {
        given: "A temporal folder "
        GroovySpy(Files, global: true)
        Files.createTempDirectory(_) >> tempFolder

        and: "leVADocumentService is mocked and Throw an exception"
        Map data = dataFixture.buildFixtureData(projectFixture)
        def initialMsgError = "Error building document: CSD with data:"
        when(leVADocumentService.createCSD(anyMap())).thenThrow(new RuntimeException(initialMsgError))

        expect: "am error in the query"
        MvcResult mvcResult = this.mockMvc
                .perform(post("/levaDoc/TEST_PROJECT_ID/2/CSD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonOutput.toJson(data)))
                .andExpect(status().isConflict()).andReturn();

        and: "msg error in the response"
        mvcResult.response.contentAsString.startsWith(initialMsgError)

        and: "the tmp folder is deleted"
        !Files.exists(tempFolder)

        where: "use valid data to generate pdf"
        projectFixture =  ProjectFixture.getProjectFixtureBuilder(getProject(), "CSD").build()
    }

    private Map getProject() {
        return [
                id:"TEST_PROJECT_ID",
                releaseId: "1",
                version: "WIP",
                validation: ""
        ]
    }

    private Map buildServiceDataParam(ProjectFixture projectFixture, Map data) {
        Map serviceParam = [:]
        serviceParam << data
        serviceParam.documentType = projectFixture.docType
        serviceParam.projectBuild = "${data.build.projectKey}-${data.build.BUILD_ID}"
        serviceParam.tmpFolder = tempFolder.toFile().absolutePath
        return serviceParam
    }

}
