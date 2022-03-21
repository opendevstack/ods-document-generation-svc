package org.ods.doc.gen.core.test.pdf

import com.github.romankh3.image.comparison.ImageComparison
import com.github.romankh3.image.comparison.model.ImageComparisonResult
import com.github.romankh3.image.comparison.model.ImageComparisonState
import groovy.util.logging.Slf4j
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Paths

@Slf4j
class PdfCompare {
    private static final String PDF = ".pdf"
    private static final String DIFF = "_diff.png"
    private static final int DPI = 300

    private final String imageDestinationPath
    private int endPage = -1

    PdfCompare(String imageDestinationPath){
        this.imageDestinationPath = imageDestinationPath
    }

    boolean compareAreEqual(String actualPdf, String expectedPdf) throws IOException{
        log.debug("actualPdf: ${actualPdf}")
        log.debug("expectedPdf: ${expectedPdf}")

        endPage = this.getPageCount(actualPdf)
        if(endPage != this.getPageCount(expectedPdf)){
            log.error("files page counts do not match - returning false")
            return false
        }
        return comparePageByPageAreEqual(actualPdf, expectedPdf, endPage)
    }

    private Boolean comparePageByPageAreEqual(String file1, String file2, int endPage) {
        def result = true
        PDDocument doc1 = null
        PDDocument doc2 = null
        ImageCompare imageCompare = new ImageCompare()
        try {
            doc1 = PDDocument.load(new File(file1))
            doc2 = PDDocument.load(new File(file2))
            PDFRenderer pdfRenderer1 = new PDFRenderer(doc1)
            PDFRenderer pdfRenderer2 = new PDFRenderer(doc2)
            for (int iPage = 0; iPage < endPage; iPage++) {
                result = result & comparePageIsEqual(pdfRenderer1, iPage, pdfRenderer2, file1, imageCompare)
            }
        } catch (Exception e) {
            throw new RuntimeException("Error comparing files", e)
        } finally {
            doc1.close()
            doc2.close()
        }

        return result
    }

    private boolean comparePageIsEqual(PDFRenderer actualPdfRenderer1, int iPage, PDFRenderer expectePpdfRenderer, String file1, ImageCompare imageCompare) {
        BufferedImage actualImage = actualPdfRenderer1.renderImageWithDPI(iPage, DPI, ImageType.RGB)
        BufferedImage expectedImage = expectePpdfRenderer.renderImageWithDPI(iPage, DPI, ImageType.RGB)
        ImageComparisonResult result = new ImageComparison(expectedImage, actualImage)
                .setDifferenceRectangleFilling(true, 50.0)
                .setRectangleLineWidth(2)
                .setPixelToleranceLevel(0.9)
                .compareImages()
        if (result.imageComparisonState != ImageComparisonState.MATCH){
            def errorFile = errorFileName(file1, iPage)
            result.writeResultTo(new File(errorFile))
            log.error("PDF error, see image file with pdf diff:[$errorFile]")
            return false
        }
        return true
    }

    private String errorFileName(String file1, int iPage) {
        Files.createDirectories(Paths.get(imageDestinationPath));
        return  this.imageDestinationPath +
            File.separator +
            new File(file1).getName().replace(PDF, "_") +
            (iPage + 1) +
            DIFF
    }

    private int getPageCount(String file) throws IOException{
        PDDocument doc = PDDocument.load(new File(file))
        int pageCount = doc.getNumberOfPages()
        doc.close()

        log.debug("file : ${file} - pageCount :${pageCount}")
        return pageCount
    }

}
