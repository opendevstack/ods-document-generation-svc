package org.ods.doc.gen.leva.doc.services

import org.apache.pdfbox.pdmodel.PDDocument
import org.ods.doc.gen.core.test.SpecHelper
import org.ods.doc.gen.core.test.fixture.FixtureHelper
import spock.lang.Ignore
import spock.lang.TempDir

class PDFUtilSpec extends SpecHelper {

    @TempDir
    public File tempFolder

    def "add watermark text"() {
        given:
        def util = new PDFUtil()
        def pdfFile = new FixtureHelper().getResource("pdf.builder/Test-1.pdf")
        def text = "myWatermark"

        when:
        def result = util.addWatermarkText(pdfFile.toPath(), text)

        then:
        try (FileInputStream fis = new FileInputStream(result.toFile())) {
            def doc = PDDocument.load(fis)
            doc.getNumberOfPages() == 1
            doc.getPage(0).getContents().text.contains(text)
            doc.close()
        }
    }

    def "merge documents"() {
        given:
        def util = new PDFUtil()

        def docFile1 = new FixtureHelper().getResource("pdf.builder/Test-1.pdf")
        def docFile2 = new FixtureHelper().getResource("pdf.builder/Test-2.pdf")

        when:
        def result = util.merge(tempFolder.absolutePath, [docFile1.toPath(), docFile2.toPath()])

        then:
        new String(result.toFile().getBytes()).startsWith("%PDF-1.4\n")

        then:
        try (FileInputStream fis = new FileInputStream(result.toFile())) {
            def doc = PDDocument.load(fis)
            doc.getNumberOfPages() == 2
            doc.close()
        }
    }
}
