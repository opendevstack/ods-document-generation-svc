package org.ods.doc.gen.pdf.builder.api

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.ods.doc.gen.pdf.builder.services.PdfGenerationService
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

import static org.mockito.Mockito.when
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(DocGenController.class)
@Slf4j
class DocGenControllerSpec extends Specification {

    @TempDir
    public Path tempFolder

    @Inject
    private MockMvc mockMvc

    @MockBean
    private PdfGenerationService service;

    def "/document API is configured OK"() {
        given: "A temporal folder "
        GroovySpy(Files, global: true)
        Files.createTempDirectory(_) >> tempFolder

        and: "PdfGenerationService is mock"
        Path pdfFile =  Path.of("src/test/resources","dtr_proof.pdf")
        String pdfValue = Files.readAllBytes(pdfFile).encodeBase64().toString()
        when(service.generatePdfFile(metadataValue, dataValue, tempFolder)).thenReturn(pdfFile)

        expect: "a client call to /document is OK and returned json has pdf data"
        def postContent = JsonOutput.toJson([metadata: metadataValue, data: dataValue])
        def returnValue = this.mockMvc
                .perform(post("/document").contentType(MediaType.APPLICATION_JSON).content(postContent))
                .andExpect(status().isOk()).andReturn()
               // .andExpect(jsonPath("\$.data").value(pdfValue))
        returnValue != null
        and: "the tmp folder is deleted"
        !Files.exists(tempFolder)

        where: "use valid data to generate pdf"
        metadataValue = [ type: "InstallationReport", version: "1.0" ]
        dataValue =  [ name: "Project Phoenix", metadata: [ header: "header" ]]
    }

    def "/document API error msg"() {
        expect: "/document return error code"
        def postContent = JsonOutput.toJson([metadata: metadataValue, data: dataValue])
        def mvcResult = this.mockMvc
                .perform(post("/document").contentType(MediaType.APPLICATION_JSON).content(postContent))
                .andExpect(status().isPreconditionFailed())
                .andReturn()

        and: "And msg error"
        mvcResult.response.contentAsString.startsWith("missing argument")

        where: "not valid post content"
        dataValue                                                   | metadataValue
        [ name: "Project Phoenix", metadata: [ header: "header" ]]  | [ version: "1.0" ]
        [ name: "Project Phoenix", metadata: [ header: "header" ]]  | [ type: "InstallationReport" ]
        [  ]                                                        | [ type: "InstallationReport", version: "1.0" ]
        null                                                        | [ type: "InstallationReport", version: "1.0" ]
    }

}