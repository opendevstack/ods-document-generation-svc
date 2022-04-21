package org.ods.doc.gen.leva.doc.fixture

import groovy.util.logging.Slf4j
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.adapters.jira.JiraService
import org.ods.doc.gen.adapters.nexus.NexusService
import org.springframework.stereotype.Service

import javax.inject.Inject

@Service
@Slf4j
class LevaDocWiremockURLMapper {

    private final NexusService nexusService
    private final JiraService jiraService
    private final BitBucketClientConfig bitBucketClientConfig

    @Inject
    LevaDocWiremockURLMapper(BitBucketClientConfig bitBucketClientConfig,
                             JiraService jiraService,
                             NexusService nexusService){

        this.bitBucketClientConfig = bitBucketClientConfig
        this.jiraService = jiraService
        this.nexusService = nexusService
    }

    void updateURLs(LevaDocWiremock levaDocWiremock){
        updateServersUrlBase(levaDocWiremock)
    }
    private void updateServersUrlBase(LevaDocWiremock levaDocWiremock) {
        nexusService.baseURL = new URIBuilder(levaDocWiremock.nexusServer.server().baseUrl()).build()
        jiraService.baseURL = new URIBuilder(levaDocWiremock.jiraServer.server().baseUrl()).build()
        bitBucketClientConfig.url = levaDocWiremock.bitbucketServer.server().baseUrl()

        log.info("[nexus][service] baseUrl: ${nexusService.baseURL}")
        log.info("[jira][service] baseUrl: ${jiraService.baseURL}")
        log.info("[bitbucket][service][clientConfig] url: ${bitBucketClientConfig.url}")
    }

}
