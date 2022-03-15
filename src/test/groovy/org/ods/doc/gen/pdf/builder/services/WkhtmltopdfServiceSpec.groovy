package org.ods.doc.gen.pdf.builder.services

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class WkhtmltopdfServiceSpec extends Specification {

    @TempDir
    public Path tempFolder

    def "execution throw error"(){
        given:
        def service = new WkhtmltopdfService()
        def documentHtmlFile = Path.of("src/test/resources","InstallationReport.html.tmpl")
        def cmd = ["wkhtmltopdf", "--encoding", "UTF-8", "--no-outline", "--print-media-type"]

        when:
        service.executeCmd(tempFolder, documentHtmlFile, cmd)

        then:
        def e = thrown(IllegalStateException)
    }
}
