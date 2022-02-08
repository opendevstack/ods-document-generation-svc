package org.ods.doc.gen

import org.ods.doc.gen.core.test.fixture.FixtureHelper
import org.ods.doc.gen.core.test.jira.JiraServiceForWireMock
import org.ods.doc.gen.external.modules.git.BitbucketService
import org.ods.doc.gen.external.modules.git.BitbucketTraceabilityUseCase
import org.ods.doc.gen.external.modules.jira.JiraService
import org.ods.doc.gen.project.data.Project
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource

import javax.cache.annotation.CacheKey
import javax.cache.annotation.CacheResult
import java.nio.file.Path
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

   /* @Primary
    @Bean
    JiraService getDocumentTemplatesRepository(){
        return jiraServiceForWireMock
    }

    class DocumentTemplatesRepository {

        Path getTemplatesForVersion()

        boolean isApplicableToSystemConfig(){
            return true
        }

        URI getURItoDownloadTemplates(String version){
            return null
        }
    }
*/
}