package org.ods.doc.gen.pdf.builder.services


import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.test.fixture.FixtureHelper
import org.ods.doc.gen.core.test.pdf.PdfCompare
import org.ods.doc.gen.pdf.builder.repository.BitBucketDocumentTemplatesRepository
import org.ods.doc.gen.pdf.builder.repository.GithubDocumentTemplatesRepository
import org.ods.doc.gen.pdf.builder.repository.WiremockDocumentRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject
import java.nio.file.Path

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes= [TestConfig.class])
class PdfGenerationServiceSpec extends Specification {

    private static final String EXPECTED_PDF = "pdf.builder/CSD-ordgp-WIP-5.pdf"
    private static final String PDF_RAW_DATA = "pdf.builder/CSD-ordgp-WIP-7.json"

    @Inject
    PdfGenerationService pdfGenerationService

    @Inject
    GithubDocumentTemplatesRepository githubDocumentTemplatesRepo

    @Inject
    BitBucketDocumentTemplatesRepository bitBucketDocumentTemplatesRepo

    @TempDir
    public Path tempFolder

    @Inject
    WiremockDocumentRepository wiremockDocumentRepository

    def cleanup(){
        wiremockDocumentRepository.tearDownWiremock()
    }

    def "generate pdf from repo: #repository"() {
        given: "a document repository"
        def jsonRawData = getJsonRawData()
        Map metadata = getMetadata()
        if (repository == "Github"){
            wiremockDocumentRepository.setUpGithubRepository(metadata.version as String)
        } else {
            wiremockDocumentRepository.setUpBitbucketRepository(metadata.version as String)
        }

        when: "generate pdf file"
        def resultFile = pdfGenerationService.generatePdfFile(metadata, jsonRawData, tempFolder)

        then: "the result is the expected pdf"
        new PdfCompare( "build/reports/pdf")
                .compareAreEqual(resultFile.toString(), new FixtureHelper().getResource(EXPECTED_PDF).absolutePath)

        where: "BB and GH repos"
        repository << ["Github", "BuiBucket"]
    }

    private Map getMetadata(){
       return [ type: "CSD-5", version: "1.2" ]
    }

    private Object getJsonRawData(){
        def pdfRawData = new FixtureHelper().getResource(PDF_RAW_DATA)
        return new JsonSlurper().parse(pdfRawData)
    }

}
