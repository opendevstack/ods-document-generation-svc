package org.ods.doc.gen.leva.doc.services

import groovy.util.logging.Slf4j
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.util.Matrix
import org.springframework.stereotype.Service

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@SuppressWarnings(['JavaIoPackageAccess', 'LineLength', 'UnnecessaryObjectReferences'])
@Slf4j
@Service
class PDFUtil {

    Path addWatermarkText(Path filePath, String text) {
        Path result = Files.createTempFile(Paths.get(filePath.toString()).parent, "temp", '.pdf')

        PDDocument doc
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            doc = PDDocument.load(fis)
            doc.getPages().each { page ->
                def font = PDType1Font.HELVETICA
                addWatermarkTextToPage(doc, page, font, text)
            }

            doc.save(result.toFile())
        } catch (e) {
            throw new RuntimeException("Error: unable to add watermark to PDF document: ${e.message}").initCause(e)
        } finally {
            if (doc) {
                doc.close()
            }
        }

        return result
    }

    Path merge(String workspacePath, List<Path> files) {
        Path tmp
        try {
            tmp = Files.createTempFile(Paths.get(workspacePath), "merge", "pdf")
            def merger = new PDFMergerUtility()
            merger.setDestinationStream(new FileOutputStream(tmp.toFile()))

            files.each { file ->
                merger.addSource(new FileInputStream(file.toFile()))
            }

            merger.mergeDocuments()
        } catch (e) {
            throw new RuntimeException("Error: unable to merge PDF documents: ${e.message}").initCause(e)
        }

        return tmp
    }

    // Courtesy of https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/util/AddWatermarkText.java
    private addWatermarkTextToPage(PDDocument doc, PDPage page, PDFont font, String text) {
        def cs
        try {
            cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)

            def fontHeight = 100 // arbitrary for short text
            def width = page.getMediaBox().getWidth()
            def height = page.getMediaBox().getHeight()
            float stringWidth = font.getStringWidth(text) / 1000 * fontHeight
            float diagonalLength = (float) Math.sqrt(width * width + height * height)
            float angle = (float) Math.atan2(height, width)
            float x = (diagonalLength - stringWidth) / 2 // "horizontal" position in rotated world
            float y = -fontHeight / 4 // 4 is a trial-and-error thing, this lowers the text a bit
            cs.transform(Matrix.getRotateInstance(angle, 0, 0))
            cs.setFont(font, fontHeight)

            def gs = new PDExtendedGraphicsState()
            gs.setNonStrokingAlphaConstant(0.05f)
            gs.setStrokingAlphaConstant(0.05f)
            gs.setBlendMode(BlendMode.MULTIPLY)
            gs.setLineWidth(3f)
            cs.setGraphicsStateParameters(gs)

            // some API weirdness here. When int, range is 0..255.
            // when float, this would be 0..1f
            cs.setNonStrokingColor(0, 0, 0)
            cs.setStrokingColor(0, 0, 0)

            cs.beginText()
            cs.newLineAtOffset(x, y)
            cs.showText(text)
            cs.endText()
        } finally {
            cs.close()
        }
    }

}
