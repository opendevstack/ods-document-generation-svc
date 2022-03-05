package org.ods.doc.gen.external.modules.nexus

import groovy.util.logging.Slf4j
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocDataFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.ProjectFixture
import org.ods.doc.gen.core.test.workspace.TestsReports
import org.ods.doc.gen.leva.doc.services.DocumentHistoryEntry
import org.ods.doc.gen.project.data.Project
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
class JobResultsDownloadFromNexusSpec extends Specification {

    @TempDir
    public File tempFolder

    @Inject
    TestsReports testsReports


    NexusService nexusService
    JobResultsDownloadFromNexus jobResultsDownloadFromNexus

    def setup() {
        String nexusUrl = System.properties["nexus.url"]
        String nexusUsername = System.properties["nexus.username"]
        String nexusPassword = System.properties["nexus.password"]

        nexusService = Spy(new NexusService(nexusUrl, nexusUsername, nexusPassword))
        jobResultsDownloadFromNexus = Spy(new JobResultsDownloadFromNexus(nexusService))
    }

    def "test downloadTestsResults"() {
        given: "A project data"
        Map<String, String> testResultsURLs = buildTestResultsUrls()
        Path tmpTargetFolder = Files.createTempDirectory("testResultsJob_" )

        when: "we try to download the test results from Nexus"
        jobResultsDownloadFromNexus.downloadTestsResults(testResultsURLs, tmpTargetFolder)

        then: "the generated PDF is as expected"
        4 * nexusService.retrieveArtifact(_,_,_,_)

        log.info(tmpTargetFolder.toString())
        int filesDownloaded = 0
        tmpTargetFolder.traverse {it ->
            if (it.toFile().exists()) {
                filesDownloaded++
            }
        }
        filesDownloaded >= 4
    }

    private Map setFixture(ProjectFixture projectFixture) {
        return dataFixture.buildFixtureData(projectFixture)
    }

    private ProjectData prepareServiceDataParam(ProjectFixture projectFixture, Map<Object, Object> data) {
        data.tmpFolder = tempFolder.absolutePath
        data.documentType = projectFixture.docType
        data.projectBuild =  "${projectFixture.project}-1"
        data.projectId = projectFixture.project
        data.buildNumber = "666"
        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        projectData.tmpFolder = tempFolder.absolutePath
        return projectData
    }

    private Map<String, String> buildTestResultsUrls() {
        return [
                "Unit": "https://nexus-ods.ocp.odsbox.lan/repository/leva-documentation/ordgp/ordgp-releasemanager/666/unit-ordgp-ordgp-releasemanager.zip",
                "Acceptance" : "https://nexus-ods.ocp.odsbox.lan/repository/leva-documentation/ordgp/ordgp-releasemanager/666/acceptance-ordgp-ordgp-releasemanager.zip",
                'Installation' : "https://nexus-ods.ocp.odsbox.lan/repository/leva-documentation/ordgp/ordgp-releasemanager/666/installation-ordgp-ordgp-releasemanager.zip",
                'Integration' : "https://nexus-ods.ocp.odsbox.lan/repository/leva-documentation/ordgp/ordgp-releasemanager/666/integration-ordgp-ordgp-releasemanager.zip",

        ]
    }
}
