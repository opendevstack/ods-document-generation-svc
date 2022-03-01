package org.ods.doc.gen.project.data

import groovy.util.logging.Slf4j
import org.ods.doc.gen.external.modules.git.BitbucketService
import org.ods.doc.gen.external.modules.jira.JiraService
import org.springframework.stereotype.Service

import javax.cache.annotation.CacheKey
import javax.cache.annotation.CacheResult
import javax.inject.Inject

@Slf4j
@Service
class Project {

    private final JiraService jira
    private final BitbucketService bitbucketService

    @Inject
    Project(JiraService jira, BitbucketService bitbucketService){
        this.jira = jira
        this.bitbucketService = bitbucketService
    }

    @CacheResult(cacheName = "projectData")
    ProjectData getProjectData(@CacheKey String projectBuildId, Map data){
        log.info("build project data for projectBuildId:${projectBuildId}")
        return new ProjectData(jira, bitbucketService).init(data).load()
    }

}
