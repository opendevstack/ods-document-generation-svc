package org.ods.doc.gen.project.data

import groovy.util.logging.Slf4j
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.external.modules.git.GitRepoDownloadService
import org.ods.doc.gen.external.modules.git.GitRepoVersionType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
class ProjectServiceSpec extends Specification {

    @TempDir
    public File tmpFolder

    @Inject
    Project project

    def setup() {
        String simpleName = this.class.simpleName
    }

    def cleanup() {
    }

    def "test getRepoContentsToFolder()"() {
        given: "A project data"
        // TODO: Setup fixture
        String projectKey = "FRML24113"
        String releaseRepo = "frml24113-release"
        String releaseRepoVersion = "master"

        when: "get a copy of the repository is called"
        project.getProjectData("1",[:])
        then: "check files are downloaded and no zip file remains there"


        // where: "Doctypes without testResults"
        // projectFixture << new DocTypeProjectFixture().getProjects()
    }
}
