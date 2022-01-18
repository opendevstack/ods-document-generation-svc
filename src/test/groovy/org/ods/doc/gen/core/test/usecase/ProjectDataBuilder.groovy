package org.ods.doc.gen.core.test.usecase

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.shared.lib.git.GitService
import org.ods.shared.lib.jenkins.PipelineSteps
import org.ods.shared.lib.jira.JiraService
import org.ods.shared.lib.project.data.ProjectData
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

@Slf4j
class ProjectDataBuilder {
    private EnvironmentVariables env
    private File tempFolder
    private GitService gitService
    private final JiraService jira
    private final PipelineSteps steps

    ProjectDataBuilder(
            EnvironmentVariables env,
            File tempFolder,
            PipelineSteps steps,
            GitService gitService,
            JiraService jira){
        this.gitService = gitService
        this.tempFolder = tempFolder
        this.steps = steps
        this.env = env
        this.jira = jira
    }

    def loadProject(Map buildParams) {
        log.info "loadProject with:[${buildParams}]"
        ProjectData projectData
        try {
            projectData = buildProject(buildParams)
            projectData.load()
            projectData.data.openshift.targetApiUrl = "https://openshift-sample"
            projectData.repositories.each { repo -> repo.metadata = loadMetadata(repo) }
        } catch(RuntimeException e){
            log.error("setup error:${e.getMessage()}", e)
            throw e
        }
        return projectData
    }

    private ProjectData buildProject(Map buildParams) {
        def tmpWorkspace = tempFolder
        System.setProperty("java.io.tmpdir", tmpWorkspace.absolutePath)
        FileUtils.copyDirectory(new File("src/test/resources/workspace/${buildParams.projectKey}"), tempFolder)

        steps.env.BUILD_ID = "1"
        steps.env.WORKSPACE = tmpWorkspace.absolutePath
        steps.env.RUN_DISPLAY_URL =""
        steps.env.version = buildParams.version
        steps.env.configItem = "Functional-Test"
        steps.env.RELEASE_PARAM_VERSION = "3.0"
        steps.env.BUILD_NUMBER = "666"
        steps.env.BUILD_URL = "https://jenkins-sample"
        steps.env.JOB_NAME = "ofi2004-cd/ofi2004-cd-release-master"

        ProjectData.METADATA_FILE_NAME = 'metadata.yml'

        def project = new ProjectData(jira, gitService).init(steps)
        project.data.metadata.id = buildParams.projectKey
        project.data.buildParams = buildParams
        project.data.git = buildGitData()
        return project
    }

    def buildGitData() {
        return  [
            commit: "1e84b5100e09d9b6c5ea1b6c2ccee8957391beec",
            url: "https://bitbucket/scm/ofi2004/ofi2004-release.git",
            baseTag: "ods-generated-v3.0-3.0-0b11-D",
            targetTag: "ods-generated-v3.0-3.0-0b11-D",
            author: "s2o",
            message: "Swingin' The Bottle",
            time: "2021-04-20T14:58:31.042152",
        ]
    }

    def loadMetadata(repo) {
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
