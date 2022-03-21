package org.ods.doc.gen.leva.doc.fixture

import de.redsix.pdfcompare.CompareResultWithPageOverflow
import de.redsix.pdfcompare.PdfComparator
import de.redsix.pdfcompare.env.SimpleEnvironment
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils

@Slf4j
class LevaDocTestValidator {

    private static final boolean GENERATE_EXPECTED_PDF_FILES = Boolean.parseBoolean(System.properties["generateExpectedPdfFiles"] as String)

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
            def actualFile = actualDoc(buildId)
            log.info("Validating pdf:${actualFile}")
            File expectedFile = expectedDoc(buildId)

            String diffFileName = pdfDiffFileName(expectedFile)
            boolean filesAreEqual = new PdfComparator(
                    expectedFile.absolutePath,
                    actualFile.absolutePath,
                    new CompareResultWithPageOverflow())
                    .withEnvironment(new SimpleEnvironment()
                            .setParallelProcessing(true)
                            .setAddEqualPagesToResult(false))
                    .compare()
                    .writeTo(diffFileName)
            if (filesAreEqual)
                new File("${diffFileName}.pdf").delete()
            return filesAreEqual
        }
    }

    String pdfDiffFileName(String buildId){
        def expectedFile = expectedDoc(buildId)
        return "${pdfDiffFileName(expectedFile)}.pdf"
    }

    String pdfDiffFileName(File expectedFile){
        return "${SAVED_DOCUMENTS}/${expectedFile.name.take(expectedFile.name.lastIndexOf('.'))}-diff"
    }

    private void unzipGeneratedArtifact(String buildId) {
        String source = "${tempFolder.absolutePath}/artifacts/${getArtifactName(buildId)}.zip"
        String destination = "${tempFolder.absolutePath}"
        log.debug("unzipGeneratedArtifact src:[${source}]")
        log.debug("unzipGeneratedArtifact dest:[${destination}]")
        new AntBuilder().unzip(src: source, dest: destination, overwrite: "true")
    }

    private String getArtifactName(buildId) {
        def comp =  (projectFixture.component) ? "${projectFixture.component}-" : ''
        def projectId = projectFixture.project
        return "${projectFixture.docType}-${ projectId.toUpperCase()}-${comp}${projectFixture.version}-${buildId}"
    }

    File expectedDoc(String buildId) {
        def comp =  (projectFixture.component) ? "${projectFixture.component}/" : ''
        def filePath = "src/test/resources/expected/${projectFixture.project.toUpperCase()}/${comp}"
        new File(filePath).mkdirs()
        return new File("${filePath}/${getArtifactName(buildId)}.pdf")
    }

    private void copyDocWhenRecording(String buildId) {
        FileUtils.copyFile(actualDoc(buildId), expectedDoc(buildId))
    }

    private File actualDoc(String buildId) {
        return new File("${tempFolder.getAbsolutePath()}/${getArtifactName(buildId)}.pdf")
    }

}
