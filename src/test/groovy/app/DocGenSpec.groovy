package app

import com.github.tomakehurst.wiremock.client.WireMock
import groovy.util.slurpersupport.GPathResult
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
        assertThat(new String(result), startsWith("%PDF-1.4\n"))

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
        def result = new DocGen().generate("InstallationReport", version, data)

        then:
        assertThat(new String(result), startsWith("%PDF-1.4\n"))
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
        def result = new DocGen().generate("DTR", version, data)

        then:
        println ("asserting generated file")
        assertThat(new String(result), startsWith("%PDF-1.4\n"))
    }

}
