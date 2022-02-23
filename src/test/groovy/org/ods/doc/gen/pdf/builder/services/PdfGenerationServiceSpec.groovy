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

    class FixtureElement {
        public final String expected
        public final String raw_data
        public final Map metadata

        FixtureElement(expected, raw_data, Map metadata) {
            this.expected = expected
            this.raw_data = raw_data
            this.metadata = metadata
        }
    }


    private static final Map [] FIXTURES = [
            [expected:"pdf.builder/CSD-ordgp-WIP-5.pdf", raw_data:"pdf.builder/CSD-ordgp-WIP-7.json", metadata: [ type: "CSD-5", version: "1.2" ]],
            [expected:"pdf.builder/CFTP-ordgp-WIP-8.pdf", raw_data:"pdf.builder/CFTP-ordgp-WIP-8.json", metadata: [ type: "CFTP-5", version: "1.2" ]]
    ]

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

    def "generate pdf from GH template #fixtureElement.metadata.version, doc type #fixtureElement.metadata.type and repo: #repository"() {
        given: "a document repository"
        def jsonRawData = getJsonRawData(fixtureElement.raw_data as String)
        String metadataVersion = fixtureElement.metadata.version as String

        if (repository == "Github"){
            wiremockDocumentRepository.setUpGithubRepository(metadataVersion)
        } else {
            wiremockDocumentRepository.setUpBitbucketRepository(metadataVersion)
        }

        when: "generate pdf file"
        Path resultFile = pdfGenerationService.generatePdfFile(fixtureElement.metadata as Map, jsonRawData as Map, tempFolder)

        then: "the result is the expected pdf"
        comparePdfs(fixtureElement.expected as String, resultFile)

        where: "BB and GH repos"
        fixtureElement << FIXTURES

        repository = "Github"
    }

    def "generate pdf from BB template #fixtureElement.metadata.version, doc type #fixtureElement.metadata.type"() {
        given: "a document repository"
        def jsonRawData = getJsonRawData(fixtureElement.raw_data as String)
        String metadataVersion = fixtureElement.metadata.version as String

        if (repository == "Github"){
            wiremockDocumentRepository.setUpGithubRepository(metadataVersion)
        } else {
            wiremockDocumentRepository.setUpBitbucketRepository(metadataVersion)
        }

        when: "generate pdf file"
        Path resultFile = pdfGenerationService.generatePdfFile(fixtureElement.metadata as Map, jsonRawData as Map, tempFolder)

        then: "the result is the expected pdf"
        comparePdfs(fixtureElement.expected as String, resultFile)

        where: "BB and GH repos"
        fixtureElement << FIXTURES

        repository = "BitBucket"
    }

    private Object getJsonRawData(String raw_data){
        def pdfRawData = new FixtureHelper().getResource(raw_data)
        return new JsonSlurper().parse(pdfRawData)
    }

    private comparePdfs(String expected, Path resultFile) {
        new PdfCompare( "build/reports/pdf")
                .compareAreEqual(resultFile.toString(), new FixtureHelper().getResource(expected).absolutePath)
    }
}
