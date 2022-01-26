package org.ods.doc.gen.pdf.builder.services


import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.ods.TestConfig
import org.ods.doc.gen.WireMockFacade
import org.ods.doc.gen.pdf.builder.repository.BitBucketDocumentTemplatesRepository
import org.ods.doc.gen.pdf.builder.repository.GithubDocumentTemplatesRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.startsWith

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes= [TestConfig.class])
class PdfGenerationServiceSpec extends Specification {

    @Inject
    PdfGenerationService pdfGenerationService

    @Inject
    GithubDocumentTemplatesRepository githubDocumentTemplatesRepository

    @Inject
    BitBucketDocumentTemplatesRepository bitBucketDocumentTemplatesRepository

    @TempDir
    public Path tempFolder

    WireMockFacade wireMockFacade = new WireMockFacade()
    EnvironmentVariables env = new EnvironmentVariables()

    def setup(){
        env.setup();
    }

    def cleanup(){
        env.teardown()
        wireMockFacade.stopWireMockServer()
    }

    def "generate pdf from repo: #repository"() {
        given: "a document repository"
        if (repository == "Github"){
            setUpGithubRepository(metadata.version as String)
        } else {
            setUpBitbucketRepository(metadata.version as String)
        }

        when: "generate pdf file"
        def resultFile = pdfGenerationService.generatePdfFile(getMetadata(), getData(), tempFolder)
        def result = Files.readAllBytes(resultFile)

        then: "the result is the expected pdf"
        assertThat(new String(result), startsWith("%PDF-1.4\n"))
        checkResult(result)

        where: "BB and GH repos"
        repository << ["Github", "BuiBucket"]
    }

    private LinkedHashMap<String, String> getMetadata() {
        [
                version: "1.0",
                type   : "InstallationReport"
        ]
    }

    private LinkedHashMap<String, Serializable> getData() {
        [
                name    : "Project Phoenix",
                metadata: [
                        header: "header",
                ],
                data    : [
                        testFiles: testFilesResults()
                ]
        ]
    }

    private void testFilesResults() {
        def xunitresults = new FileNameFinder().getFileNames('src/test/resources/data', '*.xml')
        def xunits = [[:]]
        xunitresults.each { xunit ->
            log.info ("Using file: ${xunit}")
            File xunitFile = new File(xunit)
            xunits << [name: xunitFile.name, path: xunitFile.path, text: XmlUtil.serialize(xunitFile.text)]
        }
    }

    private void setUpGithubRepository(String version) {
        setupGitHubEnv()
        mockTemplatesZipArchiveDownload(githubDocumentTemplatesRepository.getURItoDownloadTemplates(version), "ods-document-generation-templates-github.zip")
    }

    private void setUpBitbucketRepository(String version) {
        setupBitBuckectEnv()
        mockTemplatesZipArchiveDownload(bitBucketDocumentTemplatesRepository.getURItoDownloadTemplates(version), "ods-document-generation-templates-bitbucket.zip")
    }

    private setupBitBuckectEnv() {
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT", "myProject")
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_REPO", "myRepo")
        env.set("BITBUCKET_URL", "http://localhost:9001")
    }

    private setupGitHubEnv() {
        env.set("GITHUB_HOST", "http://localhost:9001")
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

    private void mockTemplatesZipArchiveDownload(URI uri, String templatesName, int returnStatus = 200) {
        def zipArchiveContent = getResource(templatesName).readBytes()
        startWiremock(uri, zipArchiveContent, returnStatus)
    }

    private void mockGithubTemplatesZipArchiveDownload(URI uri) {
        def zipArchiveContent = getResource("github-ods-document-generation-templates-1.0.zip").readBytes()
        startWiremock(uri, zipArchiveContent)
    }

    private StubMapping startWiremock(URI uri, byte[] zipArchiveContent, int returnStatus = 200) {
        wireMockFacade.startWireMockServer(uri).stubFor(WireMock.get(urlPathMatching(uri.getPath()))
                .withHeader("Accept", equalTo("application/octet-stream"))
                .willReturn(aResponse()
                        .withBody(zipArchiveContent)
                        .withStatus(returnStatus)
                ))
    }

    private File getResource(String name) {
        new File(getClass().getClassLoader().getResource(name).getFile())
    }

}
