package org.ods.doc.gen.pdf.builder.services

import org.ods.doc.gen.pdf.builder.util.OSService
import spock.lang.Specification

import java.nio.file.Path

class HtmlToPDFServiceIT extends Specification {

    def service = new HtmlToPDFService()

    OSService OSService = Mock()

    def setup() {
        service.OSService = OSService
    }

    def "execution throw error"(){
        given:
        def documentHtmlFile = Path.of("src/test/resources","InstallationReport.html.tmpl")
        def cmd = ["wkhtmltopdf", "--encoding", "UTF-8", "--no-outline", "--print-media-type"]

        when:
        service.executeCmd(documentHtmlFile, cmd)

        then:
        def e = thrown(IllegalStateException)
    }

    def "getServiceName for Windows"() {
        when:
        OSService.getOSApplicationsExtension() >> ".exe"

        then:
        service.getServiceName() == "wkhtmltopdf.exe"
    }

    def "getServiceName for not windows OS"() {
        when:
        OSService.getOSApplicationsExtension() >> ""

        then:
        service.getServiceName() == "wkhtmltopdf"
    }
}
