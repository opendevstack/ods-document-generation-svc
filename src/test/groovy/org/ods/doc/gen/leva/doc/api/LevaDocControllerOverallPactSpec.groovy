package org.ods.doc.gen.leva.doc.api

import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import groovy.util.logging.Slf4j
import org.ods.doc.gen.AppConfiguration
import org.ods.doc.gen.TestConfig
import org.ods.doc.gen.core.test.fixture.FixtureHelper
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocDataFixture
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.LevaDocTestValidator
import org.ods.doc.gen.core.test.usecase.levadoc.fixture.ProjectFixture
import org.ods.doc.gen.core.test.workspace.TestsReports
import org.ods.doc.gen.leva.doc.services.LevaDocWiremock
import org.ods.doc.gen.project.data.Project
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path

/**
 * https://github.com/pact-foundation/pact-jvm/issues/1384
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes=[TestConfig.class, AppConfiguration.class])
@ActiveProfiles('test')
class LevaDocControllerOverallPactSpec extends Specification {

    private static String PACT_FILE = 'pacts/buildDocument.overall-createDoc.overall.json'

    @Shared
    private static ProviderInfo levaDocControllerPact

    @Shared
    private static  ProviderVerifier verifier

    @Inject
    LevaDocWiremock levaDocWiremock

    @Inject
    Project project

    @Inject
    TestsReports testsReports

    @LocalServerPort
    Integer port

    @TempDir
    public Path tempFolder

    @Shared
    private LevaDocTestValidator testValidator
    private LevaDocDataFixture dataFixture
    private ProjectFixture projectFixture

    def setupSpec() {
        verifier = new ProviderVerifier()
        levaDocControllerPact = new ProviderInfo('createDoc.defaultParams')
        levaDocControllerPact.hasPactWith('buildDocument.defaultParams') { consumer ->
            consumer.pactSource = new FileSource(new FixtureHelper().getResource(PACT_FILE))
        }
    }

    def setup() {
        levaDocControllerPact.protocol = 'http'
        levaDocControllerPact.host = 'localhost'
        levaDocControllerPact.port = port
        levaDocControllerPact.path = '/'
        dataFixture = new LevaDocDataFixture(tempFolder.toFile(), null, testsReports)
        testValidator = new LevaDocTestValidator(tempFolder.toFile())
    }

    def cleanup() {
        levaDocWiremock.tearDownWiremock()
    }

    def "Provider Pact - With Consumer #consumer"() {
        given: "Initial data for the project "
        GroovySpy(Files, global: true)
        Files.createTempDirectory(_) >> tempFolder
        consumer.stateChange = { ProviderState state -> setUpFixture(state)}

        when: "Verify the contract"
        Map failures = verifyConsumerPact(consumer)

        then:
        assert failures.size() == 0 : "Error in contract:${failures}"

        //testValidator.validatePDF(projectFixture, "2022-01-22_23-59-59")

        where:
        consumer << levaDocControllerPact.consumers
    }

    private Map verifyConsumerPact(ConsumerInfo consumer) {
        verifier.initialiseReporters(levaDocControllerPact)
        Map failures = [:]
        verifier.runVerificationForConsumer(failures, levaDocControllerPact, consumer)
        return failures
    }

    private void setUpFixture(ProviderState state) {
        log.info("stateChangeHandler")
        log.info("stateChangeHandler state:${state}")
        projectFixture = buildProjectFixture(state.params)
        levaDocWiremock.setUpWireMock(projectFixture, tempFolder.toFile())
    }

    private ProjectFixture buildProjectFixture(Map<String, Object> params) {
        return ProjectFixture.builder().project(params.project).version(params.version).docType(params.docType).build()
    }

}
