package org.ods.doc.gen.leva.doc.fixture

import de.redsix.pdfcompare.CompareResultWithPageOverflow
import de.redsix.pdfcompare.PdfComparator
import de.redsix.pdfcompare.env.SimpleEnvironment
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Slf4j
class LevaDocTestValidator {

    static final boolean GENERATE_EXPECTED_PDF_FILES = Boolean.parseBoolean(System.properties["generateExpectedPdfFiles"] as String)
    static final String SAVED_DOCUMENTS = "build/reports/LeVADocs"

    private final File tempFolder
    private final ProjectFixture projectFixture

    LevaDocTestValidator(File tempFolder, ProjectFixture projectFixture){
        this.tempFolder = tempFolder
        this.projectFixture = projectFixture
    }

    boolean validatePDF(String buildId) {
        unzipGeneratedArtifact(buildId)
        if (GENERATE_EXPECTED_PDF_FILES) {
            copyDocWhenRecording(buildId)
            return true
        } else {
            return compareFiles(buildId)
        }
    }

    private boolean compareFiles(String buildId) {
        String actualPath = actualDoc(buildId).absolutePath
        File expectedFile = expectedDoc(buildId, projectFixture.component)
        String expectedPath = expectedFile.absolutePath
        log.info("validatePDF - Expected pdf: ${expectedPath}")

        String diffFileName = pdfDiffFileName(expectedFile)

        boolean filesAreEqual = new PdfComparator(expectedPath, actualPath, new CompareResultWithPageOverflow())
                .withEnvironment(new SimpleEnvironment()
                        .setParallelProcessing(true)
                        .setAddEqualPagesToResult(false))
                .compare()
                .writeTo(diffFileName)
        if (filesAreEqual) {
            new File("${diffFileName}.pdf").delete()
        } else {
            FileUtils.copyFile(actualDoc(buildId), reportPdfDoc(buildId))
        }

        if (!filesAreEqual) {
            String generatedPdfSavedCopyFileName = generatedPdfSavedCopyFileName(actualPath)
            Path generatedPdfSource = java.nio.file.Paths.get(actualPath)
            Path generatedPdfSavedCopy = Paths.get(generatedPdfSavedCopyFileName)
            Files.copy(generatedPdfSource, generatedPdfSavedCopy,
                    StandardCopyOption.REPLACE_EXISTING)
            log.info("validatePDF - Built pdf (saved because different from expected): " +
                    "${generatedPdfSavedCopyFileName}")
        }

        return filesAreEqual
    }

    String pdfDiffFileName(String buildId){
        def expectedFile = expectedDoc(buildId, projectFixture.component)
        return "${pdfDiffFileName(expectedFile)}.pdf"
    }

    String pdfDiffFileName(File expectedFile){
        return "${SAVED_DOCUMENTS}/${expectedFile.name.take(expectedFile.name.lastIndexOf('.'))}-diff"
    }

    String generatedPdfSavedCopyFileName(String actualPath){
        return "${SAVED_DOCUMENTS}/generated-${actualPath.substring(actualPath.lastIndexOf('/') +1)}"
    }

    private void unzipGeneratedArtifact(String buildId) {
        String source = "${tempFolder.absolutePath}/artifacts/${getArtifactName(buildId, projectFixture.component)}.zip"
        String destination = "${tempFolder.absolutePath}"
        log.debug("unzipGeneratedArtifact src:[${source}]")
        log.debug("unzipGeneratedArtifact dest:[${destination}]")
        new AntBuilder().unzip(src: source, dest: destination, overwrite: "true")
    }

    private String getArtifactName(buildId, component = null) {
        def comp =  (component) ? "${component}-" : ''
        def projectId = projectFixture.project
        return "${projectFixture.docType}-${ projectId.toUpperCase()}-${comp}${projectFixture.version}-${buildId}"
    }

    File expectedDoc(String buildId, component = null) {
        def comp =  (component) ? "${component}/" : ''
        def filePath = "src/test/resources/expected/${projectFixture.project.toUpperCase()}/${comp}"
        new File(filePath).mkdirs()
        return new File("${filePath}/${getArtifactName(buildId, component)}.pdf")
    }

    private void copyDocWhenRecording(String buildId) {
        FileUtils.copyFile(actualDoc(buildId), expectedDoc(buildId, projectFixture.component))
    }

    private File actualDoc(String buildId) {
        return new File("${tempFolder.getAbsolutePath()}/${getArtifactName(buildId, projectFixture.component)}.pdf")
    }

    private File reportPdfDoc(String buildId) {
        return new File("${SAVED_DOCUMENTS}/${getArtifactName(buildId)}-actual.pdf")
    }
}
