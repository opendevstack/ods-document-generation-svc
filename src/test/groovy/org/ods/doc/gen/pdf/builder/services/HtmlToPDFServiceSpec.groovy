package org.ods.doc.gen.pdf.builder.services


import spock.lang.Specification

import java.nio.file.Path

class HtmlToPDFServiceSpec extends Specification {

    def "execution throw error"(){
        given:
        def service = new HtmlToPDFService()
        def documentHtmlFile = Path.of("src/test/resources","InstallationReport.html.tmpl")
        def cmd = ["wkhtmltopdf", "--encoding", "UTF-8", "--no-outline", "--print-media-type"]

        when:
        service.executeCmd(documentHtmlFile, cmd)

        then:
        def e = thrown(IllegalStateException)
    }
}
