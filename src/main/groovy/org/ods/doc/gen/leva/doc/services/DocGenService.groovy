package org.ods.doc.gen.leva.doc.services

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.adapters.nexus.NexusService
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.pdf.builder.services.PdfGenerationService
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.stereotype.Service

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Service
class DocGenService {

    public static final String CONTENT_TPE = "application/zip"

    private final PdfGenerationService pdfGenerationService
    private final ZipFacade zip
    protected final NexusService nexus
    protected final PDFService pdf

    DocGenService(PdfGenerationService pdfGenerationService,
                  ZipFacade zip,
                  NexusService nexus,
                  PDFService pdf) {
        this.zip = zip
        this.pdfGenerationService = pdfGenerationService
        this.nexus = nexus
        this.pdf = pdf
    }

    String createOverallDocument(String templateName,
                                 String documentType,
                                 Map metadata,
                                 Closure visitor = null,
                                 String watermarkText = null,
                                 ProjectData projectData) {
        def documents = []
        def sections = []

        projectData.getOverallDocsToMerge(documentType).each { Map componentPdf ->
            documents << Paths.get(componentPdf.pdfPath)
            sections << [
                    heading: "${documentType} for component: ${componentPdf.component} (merged)"
            ]
        }

        def data = [
                metadata: metadata,
                data    : [
                        sections: sections
                ],
        ]

        if (visitor) {
            visitor(data.data)
        }

        // Create a cover page and merge all documents into one
        def modifier = { document ->
            documents.add(0, document)
            return this.pdf.merge(projectData.tmpFolder as String, documents)
        }

        return createDocument(projectData, documentType, null, data, [:], modifier, templateName, watermarkText)
    }

    String createDocument(ProjectData projectData,
                          String documentType,
                          Map repo,
                          Map data,
                          Map<String, String> files = [:],
                          Closure modifier = null,
                          String templateName = null,
                          String watermarkText = null) {
        Path document = convertToPdf(templateName ?: documentType, getDocumentTemplatesVersion(projectData), data)

        // Apply PDF document modifications, if provided
        if (modifier) {
            document = modifier(document)
        }

        // Apply PDF document watermark, if provided
        if (watermarkText) {
            document = this.pdf.addWatermarkText(document, watermarkText)
        }

        String version = projectData.build.version
        String buildId = projectData.build.buildId
        String basename = this.getDocumentBasename(projectData, documentType, version, buildId, repo)
        String pdfName = "${basename}.pdf"

        Map<String, String> artifacts = buildArchiveWithPdfAndRawData(projectData.tmpFolder, pdfName, document, basename, data, files)
        String pathToFile = this.zip.createZipFileFromFiles(projectData.tmpFolder, "${basename}.zip", artifacts)

        if (Constants.OVERALL_DOC_TYPES.contains(documentType) && repo) {
            File pdfFile = Paths.get(projectData.tmpFolder, pdfName).toFile()
            FileUtils.copyFile(document.toFile(), pdfFile)
            projectData.addOverallDocToMerge(documentType, repo.id as String, pdfFile.absolutePath)
        }

        String directory = "${projectData.key.toLowerCase()}-${projectData.build.version}"
        URI docURL = nexus.storeArtifact(directory, "${basename}.zip", pathToFile, CONTENT_TPE)
        logUploadFile(documentType, repo, docURL)
        return docURL.toString()
    }

    private void logUploadFile(String documentType, Map repo, URI docURL) {
        String message = "Document ${documentType} created and uploaded"
        if (repo) {
            message += " for ${repo.id}"
        }
        message += " to [${docURL}]"
        log.info message
    }

    private Map<String, String> buildArchiveWithPdfAndRawData(String tmpFolder,
                                                              String pdfName,
                                                              Path document,
                                                              String basename,
                                                              Map data,
                                                              Map<String, String> files) {

        Path jsonTemp = Files.createTempFile(Paths.get(tmpFolder), basename, '.json')
        try (FileWriter writer = new FileWriter(jsonTemp.toFile())) {
            writer.write(JsonOutput.toJson(data))
        }

        Map artifacts = [
                "${pdfName}"          : document.toString(),
                "raw/${basename}.json": jsonTemp.toString(),
        ]
        artifacts << files.collectEntries { name, path ->
            [name, path]
        }
        return artifacts
    }

    private String getDocumentBasename(ProjectData projectData,
                                       String documentType,
                                       String version,
                                       String build = null,
                                       Map repo = null) {
        getDocBasenameWithDocVersion(projectData, documentType, getDocumentVersion(projectData, version, build), repo)
    }

    private String getDocBasenameWithDocVersion(ProjectData projectData,
                                                String documentType,
                                                String docVersion,
                                                Map repo = null) {
        def result = projectData.key
        if (repo) {
            result += "-${repo.id}"
        }

        return "${documentType}-${result}-${docVersion}".toString()
    }

    private String getDocumentVersion(ProjectData projectData, String projectVersion, String build = null) {
        if (build) {
            "${projectVersion}-${build}"
        } else {
            "${projectVersion}-${projectData.build.buildId}"
        }
    }

    private String getDocumentTemplatesVersion(ProjectData projectData) {
        def capability = projectData.getCapability('LeVADocs')
        return capability.templatesVersion
    }

    private Path convertToPdf(String type, String version, Map data)  {
        def body = [
                metadata: [
                        type   : type,
                        version: version
                ],
                data    : data
        ]
        Path tmpDir
        Path documentPdf
        try {
            tmpDir = Files.createTempDirectory("${body.metadata.type}-v${body.metadata.version}")
            documentPdf = pdfGenerationService.generatePdfFile(body.metadata as Map, body.data as Map, tmpDir)
        } catch (Throwable e) {
            throw new RuntimeException("Conversion form HTML to PDF failed, corrupt data.", e)
        }

        return documentPdf
    }

}
