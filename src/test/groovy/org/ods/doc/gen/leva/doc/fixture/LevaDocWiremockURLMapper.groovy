package org.ods.doc.gen.leva.doc.fixture

import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.core.URLHelper
import org.ods.doc.gen.external.modules.jira.JiraService
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.leva.doc.fixture.LevaDocWiremock
import org.springframework.stereotype.Service

import javax.inject.Inject

@Service
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

    void updateURLs(LevaDocWiremock levaDocWiremock, Map data){
        updateServersUrlBase(levaDocWiremock)
        updateDataURL(data)
    }
    private void updateServersUrlBase(LevaDocWiremock levaDocWiremock) {
        nexusService.baseURL = new URIBuilder(levaDocWiremock.nexusServer.server().baseUrl()).build()
        jiraService.baseURL = new URIBuilder(levaDocWiremock.jiraServer.server().baseUrl()).build()
        bitBucketClientConfig.url = levaDocWiremock.bitbucketServer.server().baseUrl()
    }

    private void updateDataURL(Map data) {
        data.build.jenkinLog = URLHelper.replaceHostInUrl(data.build.jenkinLog as String, nexusService.baseURL.toString())
        data.build.testResultsURLs = updateMapNexusUrl(data.build.testResultsURLs)
    }

    private Map updateMapNexusUrl(Map nexusUrls) {
        Map updatedUrls = [:]
        nexusUrls.each {entry ->
            updatedUrls[entry.key] = URLHelper.replaceHostInUrl(entry.value as String, nexusService.baseURL.toString())
        }
        return updatedUrls
    }

}
