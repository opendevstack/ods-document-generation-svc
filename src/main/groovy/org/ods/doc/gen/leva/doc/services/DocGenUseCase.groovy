package org.ods.doc.gen.leva.doc.services

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.ods.shared.lib.jenkins.JenkinsService
import org.ods.shared.lib.jenkins.PipelineUtil
import org.ods.shared.lib.nexus.NexusService
import  org.ods.shared.lib.project.data.Project
import org.ods.shared.lib.project.data.ProjectData

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
    protected final PipelineUtil util
    protected final DocGenService docGen
    protected final NexusService nexus
    protected final PDFUtil pdf
    protected final JenkinsService jenkins

    DocGenUseCase(Project project, PipelineUtil util, DocGenService docGen, NexusService nexus, PDFUtil pdf, JenkinsService jenkins) {
        this.project = project
        this.util = util
        this.docGen = docGen
        this.nexus = nexus
        this.pdf = pdf
        this.jenkins = jenkins
    }

    String createDocument(ProjectData projectData, String documentType, Map repo, Map data, Map<String, byte[]> files = [:], Closure modifier = null, String templateName = null, String watermarkText = null) {
        // Create a PDF document via the DocGen service
        def document = this.docGen.createDocument(templateName ?: documentType, this.getDocumentTemplatesVersion(projectData), data)

        // Apply PDF document modifications, if provided
        if (modifier) {
            document = modifier(document)
        }

        // Apply PDF document watermark, if provided
        if (watermarkText) {
            document = this.pdf.addWatermarkText(document, watermarkText)
        }

        def basename = this.getDocumentBasename(projectData, documentType, projectData.buildParams.version, projectData.data.env.BUILD_ID, repo)
        def pdfName = "${basename}.pdf"

        // Create an archive with the document and raw data
        def artifacts = [
            "${pdfName}": document,
            "raw/${basename}.json": JsonOutput.toJson(data).getBytes(),
        ]
        artifacts << files.collectEntries { path, contents ->
            [ path, contents ]
        }

        def doCreateArtifact = shouldCreateArtifact(documentType, repo)
        def artifact = this.util.createZipArtifact(
                projectData,
            "${basename}.zip",
            artifacts,
            doCreateArtifact
        )

        // Concerns DTR/TIR for a single repo
        if (!doCreateArtifact) {
            this.util.createAndStashArtifact(projectData, pdfName, document)
            if (repo) {
                repo.data.documents[documentType] = pdfName
            }
        }

        // Store the archive as an artifact in Nexus
        def uri = this.nexus.storeArtifact(
            projectData.services.nexus.repository.name,
            "${projectData.key.toLowerCase()}-${projectData.buildParams.version}",
            "${basename}.zip",
            artifact,
            "application/zip"
        )

        def message = "Document ${documentType}"
        if (repo) {
            message += " for ${repo.id}"
        }
        message += " uploaded @ ${uri}"
        log.info message
        return uri.toString()
    }

    @SuppressWarnings(['JavaIoPackageAccess'])
    String createOverallDocument(String templateName, String documentType, Map metadata, Closure visitor = null, String watermarkText = null, ProjectData projectData) {
        def documents = []
        def sections = []

        projectData.repositories.each { repo ->
            def documentName = repo.data.documents[documentType]

            if (documentName) {
                def path = "${projectData.data.env.WORKSPACE}/reports/${repo.id}"
                jenkins.unstashFilesIntoPath(documentName, path, documentType)

                documents << new File("${path}/${documentName}").readBytes()

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

        // Apply any data transformations, if provided
        if (visitor) {
            visitor(data.data)
        }

        // Create a cover page and merge all documents into one
        def modifier = { document ->
            documents.add(0, document)
            return this.pdf.merge(projectData.data.env.WORKSPACE as String, documents)
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
            "${projectVersion}-${projectData.data.env.BUILD_ID}"
        }
    }

    @SuppressWarnings(['AbcMetric'])
    Map resurrectAndStashDocument(ProjectData projectData, String documentType, Map repo, boolean stash = true) {
        if (!repo.data.openshift.deployments) {
            return [found: false]
        }
        String resurrectedBuild
        if (repo.data.openshift.resurrectedBuild) {
            resurrectedBuild = repo.data.openshift.resurrectedBuild
            log.info "Using ${documentType} from jenkins build: ${resurrectedBuild}" +
                " for repo: ${repo.id}"
        } else {
            return [found: false]
        }
        def buildVersionKey = resurrectedBuild.split('/')
        if (buildVersionKey.size() != 2) {
            return [found: false]
        }

        def oldBuildVersion = buildVersionKey[0]
        def basename = getDocumentBasename(
            documentType, oldBuildVersion, buildVersionKey[1], repo)
        def path = "${this.steps.env.WORKSPACE}/reports/${repo.id}"

        def fileExtensions = getFiletypeForDocumentType(documentType)
        String storageType = fileExtensions.storage ?: 'zip'
        String contentType = fileExtensions.content ?: 'pdf'
        log.info "Resolved documentType '${documentType}'" +
            " - storage/content formats: ${fileExtensions}"

        String contentFileName = "${basename}.${contentType}"
        String storedFileName = "${basename}.${storageType}"
        Map documentFromNexus =
            this.nexus.retrieveArtifact(
                projectData.services.nexus.repository.name,
                "${projectData.key.toLowerCase()}-${oldBuildVersion}",
                storedFileName, path)

        log.info "Document found: ${storedFileName} \r${documentFromNexus}"
        byte [] resurrectedDocAsBytes
        if (storageType == 'zip') {
            resurrectedDocAsBytes = this.util.extractFromZipFile(
                "${path}/${storedFileName}", contentFileName)
        } else {
            resurrectedDocAsBytes = documentFromNexus.content.getBytes()
        }

        // stash doc with new name / + build id
        if (stash) {
            this.util.createAndStashArtifact(contentFileName, resurrectedDocAsBytes)
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
            "${projectData.key.toLowerCase()}-${projectData.buildParams.version}",
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
