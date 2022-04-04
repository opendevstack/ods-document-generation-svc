package org.ods.doc.gen.leva.doc.fixture

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.core.test.workspace.TestsReports
import org.ods.doc.gen.leva.doc.services.Constants
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

    Map getModuleData(ProjectFixture projectFixture) {
        return RepoDataBuilder.getRepoForComponent(projectFixture.component)
    }

    void updateExpectedComponentDocs(ProjectData projectData, Map data, ProjectFixture projectFixture) {
        projectFixture.components.each {String component ->
            log.info("Moving pdf component: ${component} to merge into Overall pdf")
            String pdfToTemp = copyPdfToTemp(projectFixture, component, data)
            projectData.addOverallDocToMerge(projectFixture.docType, component, pdfToTemp)
        }
    }

    private Map<String, String> buildJobParams(ProjectFixture projectFixture){
        String projectWithBuild = "${projectFixture.project}/${projectFixture.buildNumber}"
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
                jobName : "${projectFixture.project}-cd/${projectFixture.project}-releasemanager",
                testResultsURLs: getAllTestResults(projectFixture, projectWithBuild),
                jenkinLog: getJenkinsLogUrl(projectWithBuild)
        ]
    }

    private String getJenkinsLogUrl(String projectWithBuild) {
        return "/repository/leva-documentation/${projectWithBuild}/jenkins-job-log.zip"
    }

    /**
     * ATTENTION!! -> As we don't have control over the parameters,
     *                we have to think that always we have all tests types
     */
    private Map getAllTestResults(ProjectFixture projectFixture, String projectWithBuild) {
        Map hardcodedUrls = [:]
        String type = "unit-${projectFixture.component}"
        hardcodedUrls << [(type): "/repository/leva-documentation/${projectWithBuild}/${type}.zip"]
        hardcodedUrls << ['installation': "/repository/leva-documentation/${projectWithBuild}/installation.zip"]
        hardcodedUrls << [
                "acceptance"  : "/repository/leva-documentation/${projectWithBuild}/acceptance.zip",
                'integration' : "/repository/leva-documentation/${projectWithBuild}/integration.zip",
        ]
         
        return hardcodedUrls
    }

    private Map<String, String> buildGitData(ProjectFixture projectFixture) {
        return  [
                commit: "1e84b5100e09d9b6c5ea1b6c2ccee8957391beec",
                baseTag: "ods-generated-v3.0-3.0-0b11-D",
                targetTag: "ods-generated-v3.0-3.0-0b11-D",
                author: "ODS Jenkins Shared Library System User (undefined)",
                message: "Swingin' The Bottle",
                time: "2021-04-20T14:58:31.042152",
                releaseManagerRepo: "${projectFixture.releaseManagerRepo}",
                releaseManagerBranch: "${projectFixture.releaseManagerBranch}",
        ]
    }

    private String copyPdfToTemp(ProjectFixture projectFixture, String component, Map data) {
        String destPath = "${tempFolder}/reports/${component}"
        new File(destPath).mkdirs()
        LevaDocTestValidator testValidator = new LevaDocTestValidator(tempFolder, projectFixture)
        File expected = testValidator.expectedDoc(data.build.buildId as String, component)
        File tempPdf = new File("${destPath}/${expected.name}")
        FileUtils.copyFile(expected, tempPdf)
        return tempPdf.absolutePath
    }

}
