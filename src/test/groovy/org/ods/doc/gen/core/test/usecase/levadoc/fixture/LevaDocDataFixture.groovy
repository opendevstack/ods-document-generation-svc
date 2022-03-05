package org.ods.doc.gen.core.test.usecase.levadoc.fixture

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.core.test.usecase.RepoDataBuilder
import org.ods.doc.gen.core.test.wiremock.WiremockManager
import org.ods.doc.gen.core.test.wiremock.WiremockServers
import org.ods.doc.gen.core.test.workspace.TestsReports
import org.ods.doc.gen.project.data.Project
import org.ods.doc.gen.project.data.ProjectData

@Slf4j
class LevaDocDataFixture {

    private final File tempFolder
    private final Project project
    private final TestsReports testsReports

    LevaDocDataFixture(File tempFolder,
                       Project project = null,
                       TestsReports testsReports = null){
        this.tempFolder = tempFolder
        this.testsReports = testsReports
        this.project = project
    }

    Map buildFixtureData(ProjectFixture projectFixture){
        Map data = [:]
        data.build = buildJobParams(projectFixture)
        data.git =  buildGitData(projectFixture)
        data.openshift = [targetApiUrl:"https://openshift-sample"]
        return data
    }

    Map getModuleData(ProjectFixture projectFixture, Map data) {
        Map input = RepoDataBuilder.getRepoForComponent(projectFixture.component)
        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        input.data.tests << [unit: testsReports.getResults(projectData, projectFixture.component, "unit")]
        return input
    }

    void updateExpectedComponentDocs(ProjectData projectData, Map data, ProjectFixture projectFixture) {
        projectData.repositories.each {repo ->
            projectFixture.component = repo.id
            repo.data.documents = (repo.data.documents)?: [:]

            // see @DocGenUseCase#createOverallDocument -> unstashFilesIntoPath
            repo.data.documents[projectFixture.docType] =  copyPdfToTemp(projectFixture, data)
        }
        projectFixture.component = null
    }


    private Map<String, String> buildJobParams(ProjectFixture projectFixture){
        return  [
                targetEnvironment: "dev",
                targetEnvironmentToken: "D",
                version: "${projectFixture.version}",
                configItem: "BI-IT-DEVSTACK",
                changeDescription: "UNDEFINED",
                changeId: "1.0",
                rePromote: "false",
                releaseStatusJiraIssueKey: projectFixture.releaseKey,
                runDisplayUrl : "",
                releaseParamVersion : "3.0",
                buildId : "2022-01-22_23-59-59",
                buildURL : "https://jenkins-sample",
                jobName : "ordgp-cd/${projectFixture.project}-releasemanager",
                testResultsURLs: buildTestResultsUrls(),
        ]
    }

    private Map<String, String> buildTestResultsUrls() {
        return [
                "Unit": "https://nexus-ods.ocp.odsbox.lan/repository/leva-documentation/ordgp/ordgp-releasemanager/666/unit-ordgp-ordgp-releasemanager.zip",
                "Acceptance" : "https://nexus-ods.ocp.odsbox.lan/repository/leva-documentation/ordgp/ordgp-releasemanager/666/acceptance-ordgp-ordgp-releasemanager.zip",
                'Installation' : "https://nexus-ods.ocp.odsbox.lan/repository/leva-documentation/ordgp/ordgp-releasemanager/666/installation-ordgp-ordgp-releasemanager.zip",
                'Integration' : "https://nexus-ods.ocp.odsbox.lan/repository/leva-documentation/ordgp/ordgp-releasemanager/666/integration-ordgp-ordgp-releasemanager.zip",
        ]
    }

    private Map<String, String> buildGitData(ProjectFixture projectFixture) {
        String bitbucketUrl = System.properties["bitbucket.url"]?: "https://bitbucket-dev.biscrum.com"
        return  [
                commit: "1e84b5100e09d9b6c5ea1b6c2ccee8957391beec",
                baseTag: "ods-generated-v3.0-3.0-0b11-D",
                targetTag: "ods-generated-v3.0-3.0-0b11-D",
                author: "ODS Jenkins Shared Library System User (undefined)",
                message: "Swingin' The Bottle",
                time: "2021-04-20T14:58:31.042152",
                url: "${bitbucketUrl}/scm/${projectFixture.project}/${projectFixture.releaseManagerRepo}.git",
                releaseManagerRepo: "${projectFixture.releaseManagerRepo}",
                releaseManagerBranch: "${projectFixture.releaseManagerBranch}"
        ]
    }

    private String copyPdfToTemp(ProjectFixture projectFixture, Map data) {
        def destPath = "${tempFolder}/reports/${projectFixture.component}"
        new File(destPath).mkdirs()
        File expected = LevaDocTestValidator.expectedDoc(projectFixture, data.build.buildId as String)
        FileUtils.copyFile(expected, new File("${destPath}/${expected.name}"))
        return expected.name
    }

}
