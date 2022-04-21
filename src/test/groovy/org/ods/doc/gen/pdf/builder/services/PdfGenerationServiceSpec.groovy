package org.ods.doc.gen.pdf.builder.services

import de.redsix.pdfcompare.CompareResultWithPageOverflow
import de.redsix.pdfcompare.PdfComparator
import de.redsix.pdfcompare.env.SimpleEnvironment
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.test.fixture.FixtureHelper
import org.ods.doc.gen.pdf.builder.repository.WiremockDocumentRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes= [TestConfig.class])
class PdfGenerationServiceSpec extends Specification {

    public static final String REPORT_FOLDER = "build/reports/pdf"
    @Inject
    PdfGenerationService pdfGenerationService

    @TempDir
    public Path tempFolder

    @Inject
    WiremockDocumentRepository wiremockDocumentRepository

    def cleanup(){
        wiremockDocumentRepository.tearDownWiremock()
    }

    def "Generate pdf from #repository"() {
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
        comparePdfs(fixtureElement.expected as String, resultFile.toString())

        where: "BB and GH repos"
        repository << ["Github", "BitBucket"]
        fixtureElement =  [
                expected:"pdf.builder/CFTP-ordgp-WIP-8.pdf",
                raw_data:"pdf.builder/CFTP-ordgp-WIP-8.json",
                metadata: [ type: "CFTP-5", version: "1.2" ]
        ]
    }

    private Object getJsonRawData(String raw_data){
        def pdfRawData = new FixtureHelper().getResource(raw_data)
        return new JsonSlurper().parse(pdfRawData)
    }

    private comparePdfs(String expected, String resultFile) {
        new File(REPORT_FOLDER).mkdirs()
        File expectedFile = new FixtureHelper().getResource(expected)
        String expectedPath = expectedFile.absolutePath
        String fileName = FilenameUtils.removeExtension(expectedFile.name)
        String diffFileName = REPORT_FOLDER + "/${fileName}-diff"
        boolean filesAreEqual = new PdfComparator(expectedPath, resultFile, new CompareResultWithPageOverflow())
                .withEnvironment(new SimpleEnvironment()
                        .setParallelProcessing(true)
                        .setAddEqualPagesToResult(false))
                .compare().writeTo(diffFileName)
        if (filesAreEqual) {
            new File("${diffFileName}.pdf").delete()
        } else {
            String actualPdf = "${REPORT_FOLDER}/${fileName}-actual.pdf"
            FileUtils.copyFile(Paths.get(resultFile).toFile(), Paths.get(actualPdf).toFile())
        }
        return filesAreEqual
    }

}
