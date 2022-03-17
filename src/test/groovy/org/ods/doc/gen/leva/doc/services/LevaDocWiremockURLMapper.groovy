package org.ods.doc.gen.leva.doc.services

import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.external.modules.jira.JiraService
import org.ods.doc.gen.external.modules.nexus.NexusService
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
        data.build.jenkinLog = updateNexusUrl(data.build.jenkinLog)
        data.build.testResultsURLs = updateMapNexusUrl(data.build.testResultsURLs)
    }

    private Map updateMapNexusUrl(Map nexusUrls) {
        Map updatedUrls = [:]
        nexusUrls.each {entry ->
            updatedUrls[entry.key] = updateNexusUrl(entry.value)
        }
        return updatedUrls
    }

    private String updateNexusUrl(String hardcodedUrl) {
        return replaceHostInUrl(hardcodedUrl, nexusService.baseURL.toString())
    }

    private static String replaceHostInUrl(String originalUrl, String newUrl) {
        URI uri = new URI(originalUrl)
        URI newUri = new URI(newUrl)
        URI uriUPdated = new URI(newUri.getScheme(), newUri.getAuthority(), uri.getPath(), uri.getQuery(), uri.getFragment());
        return uriUPdated.toString();
    }
}
