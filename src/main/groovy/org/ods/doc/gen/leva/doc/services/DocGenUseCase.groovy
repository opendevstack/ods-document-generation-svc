package org.ods.doc.gen.leva.doc.services

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.leva.doc.repositories.ComponentPdfRepository
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.stereotype.Service

import java.nio.file.Paths

@Slf4j
@Service
class DocGenUseCase {

    private final ZipFacade zip
    protected final DocGenService docGen
    protected final NexusService nexus
    protected final PDFUtil pdf
    private final ComponentPdfRepository componentPdfRepository

    DocGenUseCase(ZipFacade zip,
                  DocGenService docGen,
                  NexusService nexus,
                  PDFUtil pdf,
                  ComponentPdfRepository componentPdfRepository) {
        this.componentPdfRepository = componentPdfRepository
        this.zip = zip
        this.docGen = docGen
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
            documents << Paths.get(componentPdf.pdfPath).toFile().readBytes()
            sections << [
                    heading: "${documentType} for component: ${componentPdf.component} (merged)"
            ]
        }

        def data = [
                metadata: metadata,
                data: [
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
                          Map<String, byte[]> files = [:],
                          Closure modifier = null,
                          String templateName = null,
                          String watermarkText = null) {
        byte[] document = docGen.createDocument(templateName ?: documentType, getDocumentTemplatesVersion(projectData), data)

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

        Map<String, Object> artifacts = buildArchiveWithPdfAndRawData(pdfName, document, basename, data, files)
        String pathToFile = this.zip.createZipFileFromFiles(projectData.tmpFolder, "${basename}.zip", artifacts)

        if (Constants.OVERALL_DOC_TYPES.contains(documentType) && repo) {
            File pdfFile = Paths.get(projectData.tmpFolder, pdfName).toFile()
            FileUtils.writeByteArrayToFile(pdfFile, document)
            projectData.addOverallDocToMerge(documentType, repo.id as String, pdfFile.absolutePath)
        }

        URI docURL = componentPdfRepository.storeDocument(projectData, "${basename}.zip", pathToFile)
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

    private Map<String, Object> buildArchiveWithPdfAndRawData(String pdfName,
                                                              byte[] document,
                                                              String basename, 
                                                              Map data,
                                                              Map<String, byte[]> files) {
        Map artifacts = [
                "${pdfName}"          : document,
                "raw/${basename}.json": JsonOutput.toJson(data).getBytes(),
        ]
        artifacts << files.collectEntries { path, contents ->
            [path, contents]
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

}
