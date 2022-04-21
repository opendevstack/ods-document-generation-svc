package org.ods.doc.gen

import org.ods.doc.gen.adapters.git.BitbucketService
import org.ods.doc.gen.adapters.jira.JiraService
import org.ods.doc.gen.adapters.nexus.NexusService
import org.ods.doc.gen.leva.doc.test.doubles.BitbucketServiceMock
import org.ods.doc.gen.leva.doc.test.doubles.NexusServiceForWireMock
import org.ods.doc.gen.leva.doc.test.doubles.JiraServiceForWireMock
import org.ods.doc.gen.leva.doc.test.doubles.WkhtmltopdfDockerService
import org.ods.doc.gen.pdf.builder.services.WkhtmltopdfService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Profile("test")
@TestConfiguration
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
@ComponentScan("org.ods.doc.gen")
class TestConfig {

    public static final String WKHTML__TO__PDF__WITH__DOCKER = 'WKHTML_TO_PDF_WITH_DOCKER'
    public static final String TRUE = 'true'

    @Primary
    @Bean
    Clock getSameInstant(){
        return Clock.fixed(Instant.parse("2022-01-21T10:15:30.00Z"), ZoneId.of("UTC"));
    }

    @Primary
    @Bean
    JiraService getJiraService(JiraServiceForWireMock jiraServiceForWireMock){
        jiraServiceForWireMock.setBaseURL(jiraServiceForWireMock.baseURL)
        return jiraServiceForWireMock
    }

    @Primary
    @Bean
    BitbucketService getBitbucketService(BitbucketServiceMock bitbucketServiceMock){
        return bitbucketServiceMock
    }

    @Primary
    @Bean
    NexusService getNexusService(NexusServiceForWireMock nexusServiceForWireMock){
        return nexusServiceForWireMock
    }

    @Primary
    @Bean
    WkhtmltopdfService getWkhtmltopdfService(WkhtmltopdfDockerService wkhtmltopdfDockerService,
                                             WkhtmltopdfService wkhtmltopdfService) {
        String wk = System.getenv(WKHTML__TO__PDF__WITH__DOCKER) ?: TRUE
        if ( TRUE == wk) {
            return wkhtmltopdfDockerService
        }
        return wkhtmltopdfService
    }

}