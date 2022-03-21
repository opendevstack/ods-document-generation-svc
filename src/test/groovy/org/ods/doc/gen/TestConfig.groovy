package org.ods.doc.gen

import org.ods.doc.gen.core.test.jira.JiraServiceForWireMock
import org.ods.doc.gen.core.test.pdf.WkhtmltopdfDockerService
import org.ods.doc.gen.external.modules.jira.JiraService
import org.ods.doc.gen.pdf.builder.services.WkhtmltopdfService
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@TestConfiguration
@ConfigurationProperties(prefix = "yaml")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
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
    WkhtmltopdfService getWkhtmltopdfService(WkhtmltopdfDockerService wkhtmltopdfDockerService,
                                             WkhtmltopdfService wkhtmltopdfService) {
        String wk = System.getenv('WKHTML_TO_PDF_WITH_DOCKER') ?: 'true'
        if ( 'true' == wk)
            return wkhtmltopdfDockerService
        return wkhtmltopdfService
    }

}