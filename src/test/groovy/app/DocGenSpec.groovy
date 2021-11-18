package app

import com.github.tomakehurst.wiremock.client.WireMock
import groovy.util.slurpersupport.GPathResult
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination

import java.nio.file.Files
import spock.lang.*

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.startsWith
import static org.junit.Assert.*

import groovy.xml.XmlUtil

class DocGenSpec extends SpecHelper {

    def setup() {
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT", "myProject")
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_REPO", "myRepo")
        env.set("BITBUCKET_URL", "http://localhost:9001")
    }

    def "Util.executeTemplate"() {
        given:
        def templateFile = Files.createTempFile("document", ".html.tmpl") << "<html>{{name}}</html>"
        def data = [ name: "Hello, Handlebars!" ]

        when:
        def result = DocGen.Util.executeTemplate(templateFile, data)

        then:
        result == "<html>Hello, Handlebars!</html>"

        cleanup:
        Files.delete(templateFile)
    }

    def "Util.convertHtmlToPDF"() {
        given:
        def documentHtmlFile = Files.createTempFile("document", ".html") << "<html>document</html>"
        def headerHtmlFile = Files.createTempFile("header", ".html") << "<html>header</html>"
        def footerHtmlFile = Files.createTempFile("footer", ".html") << "<html>footer</html>"

        def data = [
            name: "Project Phoenix",
            metadata: [
                header: "header"
            ]
        ]

        when:
        def result = DocGen.Util.convertHtmlToPDF(documentHtmlFile, headerHtmlFile, footerHtmlFile, data)

        then:
        def firstLine
        result.withReader { firstLine = it.readLine()}
        assertThat(firstLine, startsWith("%PDF-1.4"))

        cleanup:
        Files.delete(documentHtmlFile)
        Files.delete(headerHtmlFile)
        Files.delete(footerHtmlFile)
    }

    def "generate"() {
        given:
        def version = "1.0"

        def data = [
            name: "Project Phoenix",
            metadata: [
                header: "header"
            ]
        ]

        mockTemplatesZipArchiveDownload(
            new BitBucketDocumentTemplatesStore()
                .getZipArchiveDownloadURI(version)
        )

        when:
        def resultFile = new DocGen().generate("InstallationReport", version, data)

        then:
        def firstLine
        resultFile.withReader { firstLine = it.readLine()}
        assertThat(firstLine, startsWith("%PDF-1.4"))
        def is = new FileInputStream(resultFile);
        checkResult(is)

        cleanup:
        if(is!=null)is.close()
        if(resultFile!=null)resultFile.delete()
    }

    def "generateFromXunit"() {
        given:
        def version = "1.0"
        def xunitresults = new FileNameFinder().getFileNames('src/test/resources/data', '*.xml')
        def xunits = [[:]]
        xunitresults.each { xunit ->
          println ("--< Using file: ${xunit}")
          File xunitFile = new File (xunit)
          xunits << [name: xunitFile.name, path: xunitFile.path, text: XmlUtil.serialize(xunitFile.text) ]
        }

        def data = [
            name: "Project Phoenix",
            metadata: [
                header: "header",
            ],
            data : [
                testFiles : xunits
            ]
        ]

        println ("downloading templates")
        mockTemplatesZipArchiveDownload(
            new BitBucketDocumentTemplatesStore()
                .getZipArchiveDownloadURI(version)
        )

        when:
        println ("generating doc")
        def resultFile = new DocGen().generate("DTR", version, data)

        then:
        println ("asserting generated file")
        def firstLine
        resultFile.withReader { firstLine = it.readLine()}
        assertThat(firstLine, startsWith("%PDF-1.4"))

        def is = new FileInputStream(resultFile);
        checkResult(is)


        cleanup:
        if(is!=null)is.close()
        if(resultFile!=null)resultFile.delete()
    }

    private void checkResult(InputStream inputStream) {
        def resultDoc = PDDocument.load(inputStream)
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
