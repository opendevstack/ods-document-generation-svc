package org.ods.shared.lib.git

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.json.JSONArray
import org.ods.doc.gen.core.test.fixture.FixtureHelper
import org.ods.doc.gen.core.test.wiremock.BitbucketServiceMock
import org.ods.doc.gen.leva.doc.services.StringCleanup
import org.ods.doc.gen.project.data.Project
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.TempDir

import static org.assertj.core.api.Assertions.assertThat

@Slf4j
class BitbucketTraceabilityUseCaseSpec extends Specification {
    static final String EXPECTED_BITBUCKET_CSV = "expected/bitbucket.csv"
    private static final String EXPECTED_BITBUCKET_JSON = "expected/bitbucket.json"

    // Change for local development or CI testing
    private static final Boolean RECORD_WIREMOCK = false
    private static final String BB_URL_TO_RECORD = "http://bitbucket.odsbox.lan:7990/"
    private static final String BB_TOKEN = ""
    private static final String PROJECT_KEY = "EDPT3"

    @TempDir
    public File tempFolder
    def steps = [:]
    BitbucketServiceMock bitbucketServiceMock
    Project project
    BitbucketService bitbucketService

    def setup() {
        log.info "Using temporal folder:${tempFolder.absolutePath}"

        steps.env.WORKSPACE = tempFolder.absolutePath
        project = buildProject(logger)
        bitbucketServiceMock = new BitbucketServiceMock().setUp("csv").startServer(RECORD_WIREMOCK, BB_URL_TO_RECORD)
        bitbucketService = Spy(
                new BitbucketService(
                        null,
                        bitbucketServiceMock.getWireMockServer().baseUrl(),
                        PROJECT_KEY,
                        "passwordCredentialsId",
                        logger))
        bitbucketService.getToken() >> BB_TOKEN
    }

    def buildProject(logger) {
        FileUtils.copyDirectory(new FixtureHelper().getResource("workspace/metadata.yml").parentFile, tempFolder)

        steps.env.BUILD_ID = "1"
        steps.env.WORKSPACE = "${tempFolder.absolutePath}"

        def project = new Project(steps, logger, [:])
        project.data.metadata = project.loadMetadata("metadata.yml")
        project.data.metadata.id = PROJECT_KEY
        project.data.buildParams = [:]
        project.data.buildParams.targetEnvironment = "dev"
        project.data.buildParams.targetEnvironmentToken = "D"
        project.data.buildParams.version = "WIP"
        project.data.buildParams.releaseStatusJiraIssueKey = "${PROJECT_KEY}-123"
        return project
    }

    def cleanup() {
        bitbucketServiceMock.tearDown()
    }

    @Ignore // TODO s2o
    def "Generate the csv source code review file"() {
        given: "There are two Bitbucket repositories"
        def useCase = new BitbucketTraceabilityUseCase(bitbucketService, steps, project)

        when: "the source code review file is generated"
        def actualFile = useCase.generateSourceCodeReviewFile()

        then: "the generated file is as the expected csv file"
        reportInfo "Generated csv file:<br/>${readSomeLines(actualFile)}"
        def expectedFile = new FixtureHelper().getResource(EXPECTED_BITBUCKET_CSV)
        assertThat(new File(actualFile)).exists().isFile().hasSameTextualContentAs(expectedFile);
    }

    @Ignore // TODO s2o
    def "Read the csv source code review file"() {
        given: "There are two Bitbucket repositories"
        def useCase = new BitbucketTraceabilityUseCase(bitbucketService, steps, project)

        and: 'The characters to change'
        Map CHARACTERS = [
            '/': '/\u200B',
            '@': '@\u200B',
        ]
        when: "the source code review file is read"
        def data = useCase.readSourceCodeReviewFile(
            new FixtureHelper().getResource(EXPECTED_BITBUCKET_CSV).getAbsolutePath())
        JSONArray result = new JSONArray(data)

        then: "the data contains the same csv info"
        def expectedFile = new FixtureHelper().getResource(EXPECTED_BITBUCKET_JSON)
        def jsonSlurper = new JsonSlurper()
        def expected = jsonSlurper.parse(expectedFile)

        def removeCharacters = StringCleanup.removeCharacters(expected.toString(), CHARACTERS)
        JSONAssert.assertEquals(expectedFile.text, result.toString(), JSONCompareMode.LENIENT)
    }

    private String readSomeLines(String filePath){
        File file = new File(filePath)
        def someLines = 3
        String lines = ""
        file.withReader { r -> while( someLines-- > 0 && (( lines += r.readLine() + "<br/>" ) != null));}
        lines += "..."
        return lines
    }
}
