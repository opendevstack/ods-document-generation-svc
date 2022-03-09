package org.ods.doc.gen.leva.doc.services

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.project.data.Project
import org.ods.doc.gen.project.data.ProjectData

@Slf4j
@SuppressWarnings([
    'AbstractClassWithPublicConstructor',
    'LineLength',
    'ParameterCount',
    'GStringAsMapKey',
    'DuplicateMapLiteral'])
abstract class DocGenUseCase {

    static final String RESURRECTED = "resurrected"

    protected final Project project
    private final ZipFacade zip
    protected final DocGenService docGen
    protected final NexusService nexus
    protected final PDFUtil pdf

    DocGenUseCase(Project project, ZipFacade zip, DocGenService docGen, NexusService nexus, PDFUtil pdf) {
        this.project = project
        this.zip = zip
        this.docGen = docGen
        this.nexus = nexus
        this.pdf = pdf
    }

    String createDocument(ProjectData projectData,
                          String documentType,
                          Map repo,
                          Map data,
                          Map<String, byte[]> files = [:],
                          Closure modifier = null,
                          String templateName = null,
                          String watermarkText = null) {
        def document = docGen.createDocument(templateName ?: documentType, getDocumentTemplatesVersion(projectData), data)

        // Apply PDF document modifications, if provided
        if (modifier) {
            document = modifier(document)
        }

        // Apply PDF document watermark, if provided
        if (watermarkText) {
            document = this.pdf.addWatermarkText(document, watermarkText)
        }

        def basename = this.getDocumentBasename(projectData,
                documentType,
                projectData.build.version,
                projectData.build.buildId,
                repo)
        def pdfName = "${basename}.pdf"

        Map<GString, Object> artifacts = buildArchiveWithPdfAndRawData(pdfName, document, basename, data, files)
        boolean doCreateArtifact = shouldCreateArtifact(documentType, repo)
        byte[] artifact = this.zip.createZipFileFromFiles(projectData.tmpFolder, "${basename}.zip", artifacts)

        // Concerns DTR/TIR for a single repo
        if (!doCreateArtifact) {
            if (repo) {
                repo.data.documents[documentType] = pdfName
            }
        }

        // Store the archive as an artifact in Nexus
        def uri = this.nexus.storeArtifact(
            projectData.services.nexus.repository.name,
            "${projectData.key.toLowerCase()}-${projectData.build.version}",
            "${basename}.zip",
            artifact,
            "application/zip"
        )

        def message = "Document ${documentType} created and uploaded"
        if (repo) {
            message += " for ${repo.id}"
        }
        message += " to [${uri}]"
        log.info message
        return uri.toString()
    }

    private Map<String, Object> buildArchiveWithPdfAndRawData(pdfName, document, basename, Map data, Map<String, byte[]> files) {
        def artifacts = [
                "${pdfName}"          : document,
                "raw/${basename}.json": JsonOutput.toJson(data).getBytes(),
        ]
        artifacts << files.collectEntries { path, contents ->
            [path, contents]
        }
        artifacts
    }

    @SuppressWarnings(['JavaIoPackageAccess'])
    String createOverallDocument(String templateName, String documentType, Map metadata, Closure visitor = null, String watermarkText = null, ProjectData projectData) {
        def documents = []
        def sections = []

        projectData.repositories.each { repo ->
            def documentName = repo.data.documents[documentType]

            if (documentName) {
                String documentNamePdf = documentName.replaceFirst("zip", "pdf")
                def path = "${projectData.tmpFolder}/reports/${repo.id}"
                String jiraProjectKey = projectData.getJiraProjectKey()
                String version = projectData.build.version
                nexus.downloadAndExtractZip(jiraProjectKey.toLowerCase(), version, path, documentName)
                documents << new File("${path}/${documentNamePdf}").readBytes()
                sections << [
                    heading: "${documentType} for component: ${repo.id} (merged)"
                ]
            }
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

        def result = this.createDocument(projectData, documentType, null, data, [:], modifier, templateName, watermarkText)

        // Clean up previously stored documents
        projectData.repositories.each { repo ->
            repo.data.documents.remove(documentType)
        }

        return result
    }

    String getDocumentBasename(ProjectData projectData, String documentType, String version, String build = null, Map repo = null) {
        getDocumentBasenameWithDocVersion(projectData, documentType, getDocumentVersion(projectData, version, build), repo)
    }

    String getDocumentBasenameWithDocVersion(ProjectData projectData, String documentType, String docVersion, Map repo = null) {
        def result = projectData.key
        if (repo) {
            result += "-${repo.id}"
        }

        return "${documentType}-${result}-${docVersion}".toString()
    }

    String getDocumentVersion(ProjectData projectData, String projectVersion, String build = null) {
        if (build) {
            "${projectVersion}-${build}"
        } else {
            "${projectVersion}-${projectData.build.buildId}"
        }
    }

    @SuppressWarnings(['AbcMetric'])
    Map resurrectAndStashDocument(ProjectData projectData, String documentType, Map repo) {
        if (!repo.data.openshift.deployments) {
            return [found: false]
        }
        String resurrectedBuild
        if (repo.data.openshift.resurrectedBuild) {
            resurrectedBuild = repo.data.openshift.resurrectedBuild
            log.info "Using ${documentType} from jenkins build: ${resurrectedBuild} for repo: ${repo.id}"
        } else {
            return [found: false]
        }
        def buildVersionKey = resurrectedBuild.split('/')
        if (buildVersionKey.size() != 2) {
            return [found: false]
        }

        def oldBuildVersion = buildVersionKey[0]
        def basename = getDocumentBasename(documentType, oldBuildVersion, buildVersionKey[1], repo)
        def path = "${this.steps.env.WORKSPACE}/reports/${repo.id}"

        def fileExtensions = getFiletypeForDocumentType(documentType)
        String storageType = fileExtensions.storage ?: 'zip'
        String contentType = fileExtensions.content ?: 'pdf'
        log.info "Resolved documentType '${documentType}' - storage/content formats: ${fileExtensions}"

        String contentFileName = "${basename}.${contentType}"
        String storedFileName = "${basename}.${storageType}"
        Map documentFromNexus = nexus.retrieveArtifact(
                projectData.services.nexus.repository.name as String,
                "${projectData.key.toLowerCase()}-${oldBuildVersion}" as String,
                storedFileName as String,
                path)

        log.info "Document found: ${storedFileName} \r${documentFromNexus}"
        byte [] resurrectedDocAsBytes
        if (storageType == 'zip') {
            resurrectedDocAsBytes = this.zip.extractFromZipFile("${path}/${storedFileName}", contentFileName)
        } else {
            resurrectedDocAsBytes = documentFromNexus.content.getBytes()
        }

        if (!shouldCreateArtifact(documentType, repo)) {
            repo.data.documents[documentType] = contentFileName
        }

        return [
            found: true,
            'uri': documentFromNexus.uri,
            content: resurrectedDocAsBytes,
            createdByBuild: resurrectedBuild,
        ]
    }

    URI storeDocument (String documentName, byte [] documentAsBytes, String contentType) {
        return this.nexus.storeArtifact(
            projectData.services.nexus.repository.name,
            "${projectData.key.toLowerCase()}-${projectData.build.version}",
            "${documentName}",
            documentAsBytes,
            contentType
        )
    }

    abstract String getDocumentTemplatesVersion(ProjectData projectData)

    abstract Map getFiletypeForDocumentType (String documentType)

    abstract List<String> getSupportedDocuments()

    abstract boolean shouldCreateArtifact (String documentType, Map repo)

}
