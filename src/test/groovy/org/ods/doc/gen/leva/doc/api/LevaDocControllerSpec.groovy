package org.ods.doc.gen.leva.doc.api

import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import groovy.util.logging.Slf4j
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocDataFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.ProjectFixture
import org.ods.doc.gen.leva.doc.services.DocumentHistoryEntry
import org.ods.doc.gen.leva.doc.services.LeVADocumentService
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson
import static org.mockito.ArgumentMatchers.anyMap
import static org.mockito.ArgumentMatchers.argThat
import static org.mockito.Mockito.when
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
        dataFixture = new LevaDocDataFixture(tempFolder.toFile())
    }

    def "BuildDocument ok"() {
        given: "A temporal folder "
        GroovySpy(Files, global: true)
        Files.createTempDirectory(_) >> tempFolder

        and: "leVADocumentService is mocked"
        List<DocumentHistoryEntry> response = buildExpectedResponse()
        Map data = dataFixture.buildFixtureData(projectFixture)
        Map serviceParam = buildServiceDataParam(projectFixture, buildNumber, data)
        when(leVADocumentService.createCSD(argThat(map -> map == serviceParam))).thenReturn(response)

        expect: "a client call to /levaDoc/ProjectId/BuildID/docType return the url of the doc created"
        MvcResult mvcResult = this.mockMvc
                .perform(MockMvcRequestBuilders.post("/levaDoc/${projectFixture.project}/${buildNumber}/CSD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonOutput.toJson(data)))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn()

        and:
        mvcResult.response.contentAsString == JsonOutput.toJson(response)
        log.debug(prettyPrint(toJson(mvcResult.response.contentAsString)))

        and: "the tmp folder exist"
        Files.exists(tempFolder)

        where: "use valid data to generate pdf"
        projectFixture =  ProjectFixture.getProjectFixtureBuilder(getProject(), "CSD").build()
        buildNumber = "2"
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
                .perform(post("/levaDoc/${projectFixture.project}/2/CSD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonOutput.toJson(data)))
                .andExpect(status().isConflict()).andReturn()

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

    private Map buildServiceDataParam(ProjectFixture projectFixture, String buildNumber, Map data) {
        Map serviceParam = [:]
        serviceParam << data
        serviceParam.documentType = projectFixture.docType
        serviceParam.projectBuild = "${projectFixture.project}-${buildNumber}"
        serviceParam.projectId = projectFixture.project
        serviceParam.buildNumber = buildNumber
        serviceParam.tmpFolder = tempFolder.toFile().absolutePath
        return serviceParam
    }

    private List<DocumentHistoryEntry> buildExpectedResponse(){
        Map map =   [bugs: [], components: [], epics: [key: "ORDGP-124", action: "add"]]
        DocumentHistoryEntry historyEntry = new DocumentHistoryEntry(
                map,
                1,
                "1.0",
                "",
                "Initial document version.")
        return [historyEntry]
    }

}
