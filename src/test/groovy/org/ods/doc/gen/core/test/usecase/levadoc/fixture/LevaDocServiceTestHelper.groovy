package org.ods.doc.gen.core.test.usecase.levadoc.fixture

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.core.test.pdf.PdfCompare
import org.ods.doc.gen.core.test.usecase.RepoDataBuilder
import org.ods.doc.gen.core.test.workspace.TestsReports
import org.ods.doc.gen.leva.doc.services.PipelineConfig
import org.ods.shared.lib.project.data.Project
import org.ods.shared.lib.project.data.ProjectData
import org.yaml.snakeyaml.Yaml

import java.nio.file.Paths

@Slf4j
class LevaDocServiceTestHelper {

    static final String DEFAULT_TEMPLATE_VERSION = '1.2'
    protected static String METADATA_FILE_NAME = 'metadata.yml'

    private final boolean generateExpectedFiles
    private final String savedDocuments
    private final File tempFolder
    private final String simpleClassName
    private final Project project
    private final TestsReports testsReports

    LevaDocServiceTestHelper(String simpleClassName,
                             boolean generateExpectedFiles,
                             String savedDocuments,
                             File tempFolder,
                             TestsReports testsReports,
                             Project project){
        this.simpleClassName = simpleClassName
        this.generateExpectedFiles = generateExpectedFiles
        this.savedDocuments = savedDocuments
        this.tempFolder = tempFolder
        this.testsReports = testsReports
        this.project = project
    }

    Map buildFixtureData(ProjectFixture projectFixture){
        FileUtils.copyDirectory(new File("src/test/resources/workspace/${projectFixture.project}"), tempFolder)

        Map data = [:]
        data.projectBuild =  "${projectFixture.project}-1"
        data.documentType = projectFixture.docType
        data.jobParams = buildJobParams(projectFixture)
        data.git =  buildGitData()
        data.metadata = loadMetadata(tempFolder.absolutePath)
        data.openshift = [targetApiUrl:"https://openshift-sample"]
        data.env = getEnvVariables(tempFolder, projectFixture.version)

        // We need to override the value because of the cache in ProjectData
        project.getProjectData(data.projectBuild as String, data).data.env.WORKSPACE = tempFolder.absolutePath

        return data
    }

    boolean validatePDF(ProjectFixture projectFixture) {
        unzipGeneratedArtifact(projectFixture)
        if (generateExpectedFiles) {
            copyDocWhenRecording(projectFixture)
            return true
        } else {
            return new PdfCompare(savedDocuments).compareAreEqual(
                actualDoc(projectFixture).absolutePath,
                expectedDoc(projectFixture).absolutePath
            )
        }
    }

    Map getModuleData(ProjectFixture projectFixture, Map data) {
        Map input = RepoDataBuilder.getRepoForComponent(projectFixture.component)
        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        input.data.tests << [unit: testsReports.getResults(projectData, projectFixture.component, "unit")]
        return input
    }

    void useExpectedComponentDocs(Map data, ProjectFixture projectFixture) {
        project.getProjectData(data.projectBuild as String, data).repositories.each {repo ->
            projectFixture.component = repo.id
            repo.data.documents = (repo.data.documents)?: [:]
            if (DocTypeProjectFixtureWithComponent.notIsReleaseModule(repo)){
                // see @DocGenUseCase#createOverallDocument -> unstashFilesIntoPath
                repo.data.documents[projectFixture.docType] =  copyPdfToTemp(projectFixture, data)
            }
        }
        projectFixture.component = null
    }

    private String copyPdfToTemp(ProjectFixture projectFixture, Map data) {
        def destPath = "${data.env.WORKSPACE}/reports/${projectFixture.component}"
        new File(destPath).mkdirs()
        File expected = expectedDoc(projectFixture)
        FileUtils.copyFile(expectedDoc(projectFixture), new File("${destPath}/${expected.name}"))
        return expected.name
    }

    private File actualDoc(ProjectFixture projectFixture) {
        return new File("${tempFolder.getAbsolutePath()}/${getArtifactName(projectFixture)}.pdf")
    }

    private void unzipGeneratedArtifact(projectFixture) {
        new AntBuilder().unzip(
            src: "${tempFolder.absolutePath}/artifacts/${getArtifactName(projectFixture)}.zip",
            dest: "${tempFolder.absolutePath}",
            overwrite: "true")
    }

    private String getArtifactName(ProjectFixture projectFixture) {
        def comp =  (projectFixture.component) ? "${projectFixture.component}-" : ''
        def projectId = projectFixture.project
        return "${projectFixture.docType}-${ projectId}-${comp}${projectFixture.version}-1"
    }

    private void copyDocWhenRecording(ProjectFixture projectFixture) {
        FileUtils.copyFile(actualDoc(projectFixture), expectedDoc(projectFixture))
    }

    private File expectedDoc(ProjectFixture projectFixture) {
        def comp =  (projectFixture.component) ? "${projectFixture.component}/" : ''
        def filePath = "src/test/resources/expected/${simpleClassName}/${projectFixture.project}/${comp}"
        new File(filePath).mkdirs()
        return new File("${filePath}/${projectFixture.docType}-${projectFixture.version}-1.pdf")
    }

    private Map<String, String> getEnvVariables(File tmpWorkspace, String version) {
        return [
                WORKSPACE : tmpWorkspace.absolutePath,
                RUN_DISPLAY_URL : "",
                version : version,
                configItem : "Functional-Test",
                RELEASE_PARAM_VERSION : "3.0",
                BUILD_NUMBER : "666",
                BUILD_URL : "https://jenkins-sample",
                JOB_NAME : "ofi2004-cd/ofi2004-cd-release-master",
                BUILD_ID : "1"
        ]
    }

    private Map<String, String> buildGitData() {
        return  [
                commit: "1e84b5100e09d9b6c5ea1b6c2ccee8957391beec",
                url: "https://bitbucket/scm/ofi2004/ofi2004-release.git", //  new GitService().getOriginUrl()
                baseTag: "ods-generated-v3.0-3.0-0b11-D",
                targetTag: "ods-generated-v3.0-3.0-0b11-D",
                author: "s2o",
                message: "Swingin' The Bottle",
                time: "2021-04-20T14:58:31.042152",
        ]
    }

    private Map<String, String> buildJobParams(ProjectFixture projectFixture){
        return  [
                projectKey: projectFixture.project,
                targetEnvironment: "dev",
                targetEnvironmentToken: "D",
                version: "${projectFixture.version}",
                configItem: "BI-IT-DEVSTACK",
                changeDescription: "changeDescription",
                changeId: "changeId",
                rePromote: "false",
                releaseStatusJiraIssueKey: projectFixture.releaseKey
        ]
    }

    protected Map loadMetadata(String workspace){
        Map result = parseMetadataFile(workspace)
        result.description = (result.description)?: ""
        result.repositories = (result.repositories)?: ""
        updateRepositories(result)
        result.capabilities = (result.capabilities )?: []
        updateLevaDocCapability(result)
        result.environments = (result.environments)?: [:]
        return result
    }

    private void updateLevaDocCapability(Map result) {
        def levaDocsCapabilities = result.capabilities.findAll { it instanceof Map && it.containsKey('LeVADocs') }
        if (levaDocsCapabilities) {
            if (levaDocsCapabilities.size() > 1) {
                throw new IllegalArgumentException(
                        "Error: unable to parse project metadata. More than one 'LeVADoc' capability has been defined.")
            }

            def levaDocsCapability = levaDocsCapabilities.first()

            def gampCategory = levaDocsCapability.LeVADocs?.GAMPCategory
            if (!gampCategory) {
                throw new IllegalArgumentException(
                        "Error: 'LeVADocs' capability has been defined but contains no 'GAMPCategory'.")
            }

            def templatesVersion = levaDocsCapability.LeVADocs?.templatesVersion
            if (!templatesVersion) {
                levaDocsCapability.LeVADocs.templatesVersion = DEFAULT_TEMPLATE_VERSION
            }
        }
    }

    private void updateRepositories(Map result) {
        result.repositories.eachWithIndex { repo, index ->
            // Check for existence of required attribute 'repositories[i].id'
            if (!repo.id?.trim()) {
                throw new IllegalArgumentException(
                        "Error: unable to parse project meta data. Required attribute 'repositories[${index}].id' is undefined.")
            }

            repo.data = [
                    openshift: [:],
                    documents: [:],
            ]

            // Set repo type, if not provided
            if (!repo.type?.trim()) {
                repo.type = PipelineConfig.REPO_TYPE_ODS_CODE
            }

            repo.url = "gitURL getGitURLFromPath"
            repo.branch = 'master'
            repo.metadata = loadMetadataRepo(repo)
        }
    }

    private Map parseMetadataFile(String workspace) {
        String filename = METADATA_FILE_NAME
        def file = Paths.get(workspace, filename).toFile()
        if (!file.exists()) {
            throw new RuntimeException("Error: unable to load project meta data. File '${workspace}/${filename}' does not exist.")
        }

        Map result = new Yaml().load(file.text)

        // Check for existence of required attribute 'id'
        if (!result?.id?.trim()) {
            throw new IllegalArgumentException("Error: unable to parse project meta data. Required attribute 'id' is undefined.")
        }

        // Check for existence of required attribute 'name'
        if (!result?.name?.trim()) {
            throw new IllegalArgumentException("Error: unable to parse project meta data. Required attribute 'name' is undefined.")
        }
        return result
    }

    private Map<String, String> loadMetadataRepo(repo) {
        return  [
                id: repo.id,
                name: repo.name,
                description: "myDescription-A",
                supplier: "mySupplier-A",
                version: "myVersion-A",
                references: "myReferences-A"
        ]
    }

}
