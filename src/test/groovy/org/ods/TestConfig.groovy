package org.ods

import org.ods.doc.gen.core.test.fixture.FixtureHelper
import org.ods.doc.gen.core.test.jira.JiraServiceForWireMock
import org.ods.doc.gen.leva.doc.services.MROPipelineUtil
import org.ods.shared.lib.git.BitbucketService
import org.ods.shared.lib.git.BitbucketTraceabilityUseCase
import org.ods.shared.lib.jenkins.PipelineSteps
import org.ods.shared.lib.jira.JiraService
import org.ods.shared.lib.jira.JiraUseCase
import org.ods.shared.lib.project.data.Project
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@TestConfiguration
@PropertySource("classpath:application.properties")
@ComponentScan("org.ods")
class TestConfig {

    @Primary
    @Bean
    Clock getSameInstant(){
        return Clock.fixed(Instant.parse("2022-01-21T10:15:30.00Z"), ZoneId.of("UTC"));
    }

    @Primary
    @Bean
    JiraService getJiraService(JiraServiceForWireMock jiraServiceForWireMock){
        return jiraServiceForWireMock
    }

    @Primary
    @Bean
    BitbucketTraceabilityUseCase getBitbucketTraceabilityUseCase(){
        new BitbucketTraceabilityUseCaseStub(null, null, null)
    }

    class BitbucketTraceabilityUseCaseStub extends BitbucketTraceabilityUseCase {

        static final String EXPECTED_BITBUCKET_CSV = "expected/bitbucket.csv"

        BitbucketTraceabilityUseCaseStub(BitbucketService bitbucketService, PipelineSteps steps, Project project) {
            super(bitbucketService, steps, project)
        }

        @Override
        String generateSourceCodeReviewFile() {
            return new FixtureHelper().getResource(EXPECTED_BITBUCKET_CSV).getAbsolutePath()
        }

    }
    //@Bean
    //BitbucketTraceabilityUseCase getBitbucketTraceabilityUseCase(){
      //  new BitbucketTraceabilityUseCase(null, null, null)
   // }

    /*
        // Mocks generation (spock don't let you add this outside a Spec)
        JenkinsService jenkins = Mock(JenkinsService)
        jenkins.unstashFilesIntoPath(_, _, _) >> true

        BitbucketTraceabilityUseCase bbT = Spy(new BitbucketTraceabilityUseCase(null, null, null))
        bbT.generateSourceCodeReviewFile() >> getExpectedBitBucketCSV().getAbsolutePath()

          private JiraServiceForWireMock buildJiraServiceForWireMock() {
        new JiraServiceForWireMock(jiraServer.mock().baseUrl(), WiremockServers.JIRA.user, WiremockServers.JIRA.password)
    }

     */
}