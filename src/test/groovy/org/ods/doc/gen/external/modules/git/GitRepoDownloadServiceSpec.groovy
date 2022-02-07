package org.ods.doc.gen.external.modules.git

import groovy.util.logging.Slf4j
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.DocTypeProjectFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocDataFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocTestValidator
import org.ods.doc.gen.external.modules.git.BitbucketService
import org.ods.doc.gen.external.modules.git.GitRepoDownloadService
import org.ods.doc.gen.leva.doc.services.StringCleanup
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
class GitRepoDownloadServiceSpec extends Specification {

    @TempDir
    public File tmpFolder

    @Inject
    GitRepoDownloadService gitRepoDownloadService

    def setup() {
        String simpleName = this.class.simpleName
    }

    def cleanup() {
    }

    def "test getRepoContentsToFolder()"() {
        given: "A project data"
        // TODO: Setup fixture
        String project = "FRML24113"
        String releaseRepo = "frml24113-release"
        String releaseRepoVersion = "master"

        when: "some functionality needs a copy of a git repos (project + repos + branch)"

        then: "get a copy of the repository"
        String tmpFolderAbsolutePath = tmpFolder.getAbsolutePath()
        gitRepoDownloadService.getRepoContentsToFolder(project, releaseRepo, releaseRepoVersion, tmpFolderAbsolutePath, GitRepoVersionType.BRANCH)

        // where: "Doctypes without testResults"
        // projectFixture << new DocTypeProjectFixture().getProjects()
    }

}
