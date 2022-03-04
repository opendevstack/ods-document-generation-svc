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

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
class JobResultsDownloadFromNexusSpec extends Specification {

    @TempDir
    public File tempFolder

    @Inject
    TestsReports testsReports

    Project project
    private LevaDocDataFixture dataFixture
    NexusService nexusService
    JobResultsDownloadFromNexus jobResultsDownloadFromNexus

    def setup() {
        project = new Project()
        dataFixture = new LevaDocDataFixture(tempFolder, project, testsReports)

        String nexusUrl = System.properties["nexus.url"]
        String nexusUsername = System.properties["nexus.username"]
        String nexusPassword = System.properties["nexus.password"]

        nexusService = Spy(new NexusService(nexusUrl, nexusUsername, nexusPassword))
        jobResultsDownloadFromNexus = Spy(new JobResultsDownloadFromNexus(nexusService))
    }

    def "create #projectFixture.docType for project #projectFixture.project"() {
        given: "A project data"
        Map data = setFixture(projectFixture)
        prepareServiceDataParam(projectFixture, data)

        when: "we try to download the test results from Nexus"
        jobResultsDownloadFromNexus.downloadTestsResults(data)

        then: "the generated PDF is as expected"
        nexusService.retrieveArtifact(_,_,_,_)

        where: "Doctypes without testResults"
        projectFixture << new DocTypeProjectFixture().getProjects()
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

}
