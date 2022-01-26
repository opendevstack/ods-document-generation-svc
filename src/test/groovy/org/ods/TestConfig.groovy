package org.ods

import org.ods.doc.gen.core.test.fixture.FixtureHelper
import org.ods.doc.gen.core.test.jira.JiraServiceForWireMock
import org.ods.shared.lib.git.BitbucketService
import org.ods.shared.lib.git.BitbucketTraceabilityUseCase
import org.ods.shared.lib.jira.JiraService
import org.ods.shared.lib.project.data.Project
import org.ods.shared.lib.project.data.ProjectData
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
        new BitbucketTraceabilityUseCaseStub(null, null)
    }

    class BitbucketTraceabilityUseCaseStub extends BitbucketTraceabilityUseCase {

        static final String EXPECTED_BITBUCKET_CSV = "expected/bitbucket.csv"

        BitbucketTraceabilityUseCaseStub(BitbucketService bitbucketService, Project project) {
            super(bitbucketService, project)
        }

        @Override
        String generateSourceCodeReviewFile(ProjectData projectData) {
            return new FixtureHelper().getResource(EXPECTED_BITBUCKET_CSV).getAbsolutePath()
        }

    }

}