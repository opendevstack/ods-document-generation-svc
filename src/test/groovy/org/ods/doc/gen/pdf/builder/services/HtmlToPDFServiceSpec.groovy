package org.ods.doc.gen.pdf.builder.services

import groovy.util.logging.Slf4j
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.pdf.builder.util.OSService
import org.ods.doc.gen.pdf.builder.util.WkHtmlToPdfInDockerService
import org.ods.doc.gen.pdf.builder.util.WkHtmlToPdfService
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Inject
import java.nio.file.Path

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes= [TestConfig.class])
class HtmlToPDFServiceSpec extends Specification {

    HtmlToPDFService htmlToPDFService = new HtmlToPDFService()

    @Shared
    WkHtmlToPdfService wkHtmlToPdfNoDockerService
    @Shared
    WkHtmlToPdfService wkHtmlToPdfInDockerServiceOn
    @Shared
    WkHtmlToPdfService wkHtmlToPdfInDockerServiceOff

    OSService OSService = Mock()

    def setup() {
        wkHtmlToPdfNoDockerService = new WkHtmlToPdfService()
        wkHtmlToPdfInDockerServiceOn = new WkHtmlToPdfInDockerService("true")
        wkHtmlToPdfInDockerServiceOff = new WkHtmlToPdfInDockerService("false")
    }

    def "execution throw error"(){
        given:
        htmlToPDFService.wkHtmlToPdfService = wkHtmlToPdfNoDockerService
        def documentHtmlFile = Path.of("src/test/resources","InstallationReport.html.tmpl")
        def cmd = ["wkhtmltopdf", "--encoding", "UTF-8", "--no-outline", "--print-media-type"]

        when:
        htmlToPDFService.executeCmd(documentHtmlFile, cmd)

        then:
        def e = thrown(IllegalStateException)
    }

    def "getServiceCmd for Windows (without docker)"() {
        when:
        wkHtmlToPdfService.OSService = OSService
        OSService.getOSApplicationsExtension() >> ".exe"

        then:
        wkHtmlToPdfService.getServiceCmd() == [ "wkhtmltopdf.exe" ]

        where:
        wkHtmlToPdfService << [ wkHtmlToPdfNoDockerService, wkHtmlToPdfInDockerServiceOff ]
    }

    def "getServiceCmd for not windows OS (without docker)"() {
        when:
        wkHtmlToPdfService.OSService = OSService
        OSService.getOSApplicationsExtension() >> ""

        then:
        wkHtmlToPdfService.getServiceCmd() == [ "wkhtmltopdf" ]

        where:
        wkHtmlToPdfService << [ wkHtmlToPdfNoDockerService, wkHtmlToPdfInDockerServiceOff ]
    }

    def "getServiceCmd for docker in Windows OS"() {
        when:
        wkHtmlToPdfService.OSService = OSService
        OSService.getOSApplicationsExtension() >> ".exe"

        then:
        ArrayList<String> expectedResult = [ "docker.exe", "exec", "-i", _, "wkhtmltopdf" ]
        ArrayList<String> result = wkHtmlToPdfService.getServiceCmd()
        expectedResult.get(0) == result.get(0)
        expectedResult.get(1) == result.get(1)
        expectedResult.get(2) == result.get(2)
        expectedResult.get(4) == result.get(4)

        where:
        wkHtmlToPdfService << [ wkHtmlToPdfInDockerServiceOn ]
    }

    def "getServiceCmd for docker in not windows OS"() {
        when:
        wkHtmlToPdfService.OSService = OSService
        OSService.getOSApplicationsExtension() >> ""

        then:
        ArrayList<String> expectedResult = [ "docker", "exec", "-i", _, "wkhtmltopdf" ]
        ArrayList<String> result = wkHtmlToPdfService.getServiceCmd()
        expectedResult.get(0) == result.get(0)
        expectedResult.get(1) == result.get(1)
        expectedResult.get(2) == result.get(2)
        expectedResult.get(4) == result.get(4)

        where:
        wkHtmlToPdfService << [ wkHtmlToPdfInDockerServiceOn ]
    }

}
