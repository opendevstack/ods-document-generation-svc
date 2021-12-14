package app


import groovy.xml.XmlUtil
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import util.DocUtils

import java.nio.file.Files

import static org.junit.Assert.assertEquals

class DocGenSpec extends SpecHelper {

    def setup() {
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT", "myProject")
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_REPO", "myRepo")
        env.set("BITBUCKET_URL", "http://localhost:9001")
    }

    def "Util.executeTemplate"() {
        given:
        def templateFile = Files.createTempFile("document", ".html.tmpl") << "<html>{{name}}</html>"
        def result = Files.createTempFile('document', '.html')
        def data = [ name: "Hello, Handlebars!" ]

        when:
        DocGen.Util.executeTemplate(templateFile, result, data)

        then:
        result.text == "<html>Hello, Handlebars!</html>"

        cleanup:
        Files.delete(templateFile)
        Files.delete(result)
    }

    def "Util.convertHtmlToPDF"() {
        given:
        def documentHtmlFile = Files.createTempFile("document", ".html") << "<html>document</html>"

        def data = [
            name: "Project Phoenix",
            metadata: [
                header: "header"
            ]
        ]

        when:
        def result = DocGen.Util.convertHtmlToPDF(documentHtmlFile, data)

        then:
        def header = DocUtils.getPDFHeader(result)
        assertEquals('%PDF-1.4', header)
        def is = Files.newInputStream(result)
        checkResult(is)

        cleanup:
        if(is!=null)is.close()
        Files.delete(documentHtmlFile)
        if(result!=null)Files.deleteIfExists(result)
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
        def header = DocUtils.getPDFHeader(resultFile)
        assertEquals('%PDF-1.4', header)
        def is = Files.newInputStream(resultFile)
        checkResult(is)

        cleanup:
        if(is!=null)is.close()
        if(resultFile!=null)Files.deleteIfExists(resultFile)
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
        def header = DocUtils.getPDFHeader(resultFile)
        assertEquals('%PDF-1.4', header)

        def is = Files.newInputStream(resultFile)
        checkResult(is)


        cleanup:
        if(is!=null)is.close()
        if(resultFile!=null)Files.deleteIfExists(resultFile)
    }

    private static void checkResult(InputStream inputStream) {
        def resultDoc = PDDocument.load(inputStream)
        resultDoc.withCloseable { PDDocument doc ->
            doc.pages?.each { page ->
                page.getAnnotations { it instanceof PDAnnotationLink }
                        ?.each { link ->
                            def dest = link.destination
                            if (dest == null && link.action instanceof PDActionGoTo) {
                                dest = link.action.destination
                            }
                            if (dest instanceof PDPageDestination) {
                                assert dest.page != null
                            }
                        }
            }
            def catalog = doc.getDocumentCatalog()
            def dests = catalog.dests
            dests?.COSObject?.keySet()*.name.each { name ->
                def dest = dests.getDestination(name)
                if (dest instanceof PDPageDestination) {
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
                    if (dest instanceof PDPageDestination) {
                        assert dest.page != null
                    }
                    checkOutlineNode(item)
                }
            }
            def outline = catalog.documentOutline
            if (outline != null) {
                checkOutlineNode(outline)
            }
            return null
        }
    }

}
