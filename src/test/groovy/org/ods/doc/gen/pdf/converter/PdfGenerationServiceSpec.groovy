package org.ods.doc.gen.pdf.converter

import groovy.xml.XmlUtil
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.junit.Rule
import org.ods.doc.gen.AppConfig
import org.ods.doc.gen.SpecHelper
import org.ods.doc.gen.pdf.conversor.PdfGenerationService
import org.ods.doc.gen.templates.repository.BitBucketDocumentTemplatesRepository
import org.ods.doc.gen.templates.repository.GithubDocumentTemplatesRepository
import org.springframework.test.context.ContextConfiguration
import spock.lang.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.startsWith

@ContextConfiguration(classes= [AppConfig.class])
class PdfGenerationServiceSpec extends SpecHelper {

    @Inject
    PdfGenerationService pdfGenerationService

    @Inject
    GithubDocumentTemplatesRepository githubDocumentTemplatesRepository

    @Inject
    BitBucketDocumentTemplatesRepository bitBucketDocumentTemplatesRepository

    @TempDir
    public Path tempFolder

    @Rule
    EnvironmentVariables env = new EnvironmentVariables()

    def setup(){
        env.setup();
    }

    def cleanup(){
        env.teardown()
    }

    def "generate pdf from repo: #repository"() {
        given:
        def data = [
                name: "Project Phoenix",
                metadata: [
                        header: "header",
                ],
                data : [
                        testFiles : testFilesResults()
                ]
        ]
        def metadata = [
                version: "1.0",
                type:  "InstallationReport"
        ]
        if (repository == "Github"){
            githubRepository(metadata.version as String)
        } else {
            bitbucketRepository(metadata.version as String)
        }

        when:
        def resultFile = pdfGenerationService.generatePdfFile(metadata, data, tempFolder)
        def result = Files.readAllBytes(resultFile)
        then:
        assertThat(new String(result), startsWith("%PDF-1.4\n"))
        checkResult(result)

        where:
        repository << [ "Github", "BuiBucket"]

    }

    private void githubRepository(String version) {
        setupGitHub()
        mockTemplatesZipArchiveDownload(githubDocumentTemplatesRepository.getURItoDownloadTemplates(version))
    }

    private void bitbucketRepository(String version) {
        setupBitBuckect()
        mockTemplatesZipArchiveDownload(bitBucketDocumentTemplatesRepository.getURItoDownloadTemplates(version))
    }

    private setupBitBuckect() {
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT", "myProject")
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_REPO", "myRepo")
        env.set("BITBUCKET_URL", "http://localhost:9001")
    }

    private setupGitHub() {
        env.set("GITHUB_HOST", "http://localhost:9001")
    }

    private void testFilesResults() {
        def xunitresults = new FileNameFinder().getFileNames('src/test/resources/data', '*.xml')
        def xunits = [[:]]
        xunitresults.each { xunit ->
            println("--< Using file: ${xunit}")
            File xunitFile = new File(xunit)
            xunits << [name: xunitFile.name, path: xunitFile.path, text: XmlUtil.serialize(xunitFile.text)]
        }
    }

    private void checkResult(byte[] result) {
        def resultDoc = PDDocument.load(result)
        resultDoc.withCloseable { PDDocument doc ->
            doc.pages?.each { page ->
                page.getAnnotations { it.subtype == PDAnnotationLink.SUB_TYPE }
                        ?.each { PDAnnotationLink link ->
                            def dest = link.destination
                            if (dest == null && link.action?.subType == PDActionGoTo.SUB_TYPE) {
                                dest = link.action.destination
                            }
                            if (dest in PDPageDestination) {
                                assert dest.page != null
                            }
                        }
            }
            def catalog = doc.getDocumentCatalog()
            def dests = catalog.dests
            dests?.COSObject?.keySet()*.name.each { name ->
                def dest = dests.getDestination(name)
                if (dest in PDPageDestination) {
                    assert dest.page != null
                }
            }
            def checkStringDest
            checkStringDest = { node ->
                if (node) {
                    node.names?.each { name, dest -> assert dest.page != null }
                    node.kids?.each { checkStringDest(it) }
                }
            }
            checkStringDest(catalog.names?.dests)
            def checkOutlineNode
            checkOutlineNode = { node ->
                node.children().each { item ->
                    def dest = item.destination
                    if (dest == null && item.action?.subType == PDActionGoTo.SUB_TYPE) {
                        dest = item.action.destination
                    }
                    if (dest in PDPageDestination) {
                        assert dest.page != null
                    }
                    checkOutlineNode(item)
                }
            }
            def outline = catalog.documentOutline
            if (outline != null) {
                checkOutlineNode(outline)
            }
        }
    }

}
