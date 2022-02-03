package org.ods.doc.gen.core.test.usecase.levadoc.fixture

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.core.test.usecase.RepoDataBuilder
import org.ods.doc.gen.core.test.workspace.TestsReports
import org.ods.doc.gen.project.data.Project
import org.ods.doc.gen.project.data.ProjectData

@Slf4j
class LevaDocDataFixture {

    private final File tempFolder
    private final Project project
    private final String simpleClassName
    private final TestsReports testsReports

    LevaDocDataFixture(String simpleClassName,
                       File tempFolder,
                       Project project = null,
                       TestsReports testsReports = null){
        this.simpleClassName = simpleClassName
        this.tempFolder = tempFolder
        this.testsReports = testsReports
        this.project = project
    }

    Map buildFixtureData(ProjectFixture projectFixture){
        Map data = [:]
        data.build = buildJobParams(projectFixture)
        data.git =  buildGitData()
        data.openshift = [targetApiUrl:"https://openshift-sample"]
        return data
    }

    Map getModuleData(ProjectFixture projectFixture, Map data) {
        Map input = RepoDataBuilder.getRepoForComponent(projectFixture.component)
        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        input.data.tests << [unit: testsReports.getResults(projectData, projectFixture.component, "unit")]
        return input
    }

    void updateExpectedComponentDocs(Map data, ProjectFixture projectFixture) {
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

    File expectedDoc(ProjectFixture projectFixture) {
        def comp =  (projectFixture.component) ? "${projectFixture.component}/" : ''
        def filePath = "src/test/resources/expected/${simpleClassName}/${projectFixture.project}/${comp}"
        new File(filePath).mkdirs()
        return new File("${filePath}/${projectFixture.docType}-${projectFixture.version}-1.pdf")
    }

    private Map<String, String> buildJobParams(ProjectFixture projectFixture){
        return  [
                targetEnvironment: "dev",
                targetEnvironmentToken: "D",
                version: "${projectFixture.version}",
                configItem: "BI-IT-DEVSTACK",
                changeDescription: "changeDescription",
                changeId: "changeId",
                rePromote: "false",
                releaseStatusJiraIssueKey: projectFixture.releaseKey,
                RUN_DISPLAY_URL : "",
                RELEASE_PARAM_VERSION : "3.0",
                BUILD_NUMBER : "666",
                BUILD_URL : "https://jenkins-sample",
                JOB_NAME : "ofi2004-cd/ofi2004-cd-release-master",
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

    private String copyPdfToTemp(ProjectFixture projectFixture, Map data) {
        def destPath = "${tempFolder}/reports/${projectFixture.component}"
        new File(destPath).mkdirs()
        File expected = expectedDoc(projectFixture)
        FileUtils.copyFile(expectedDoc(projectFixture), new File("${destPath}/${expected.name}"))
        return expected.name
    }

}
