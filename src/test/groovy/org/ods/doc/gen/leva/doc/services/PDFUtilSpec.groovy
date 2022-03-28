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
        def result = util.addWatermarkText(pdfFile.bytes, text)

        then:
        def doc = PDDocument.load(result)
        doc.getNumberOfPages() == 1
        doc.getPage(0).getContents().text.contains(text)
        doc.close()
    }

    @Ignore
    def "convert from mardkdown document"() {
        given:
        def util = new PDFUtil()

        def docFile = new FixtureHelper().getResource("pdf.builder/Test.md")
        def result

        when:
        result = util.convertFromMarkdown(docFile, false)

        then:
        def doc = PDDocument.load(result)
        doc.getNumberOfPages() == 2
        doc.close()

        when:
        result = util.convertFromMarkdown(docFile, true)

        then:
        def docLandscape = PDDocument.load(result)
        docLandscape.getNumberOfPages() == 4
        docLandscape.close()

    }

    def "convert from Microsoft Word document"() {
        given:
        def util = new PDFUtil()

        def docFile = new FixtureHelper().getResource("pdf.builder/Test.docx")

        when:
        def result = util.convertFromWordDoc(docFile)

        then:
        def doc = PDDocument.load(result)
        doc.getNumberOfPages() == 1
        doc.close()
    }

    def "merge documents"() {
        given:
        def util = new PDFUtil()

        def docFile1 = new FixtureHelper().getResource("pdf.builder/Test-1.pdf")
        def docFile2 = new FixtureHelper().getResource("pdf.builder/Test-2.pdf")

        when:
        def result = util.merge(tempFolder.absolutePath, [docFile1.bytes, docFile2.bytes])

        then:
        new String(result).startsWith("%PDF-1.4\n")

        then:
        def doc = PDDocument.load(result)
        doc.getNumberOfPages() == 2
        doc.close()
    }
}
