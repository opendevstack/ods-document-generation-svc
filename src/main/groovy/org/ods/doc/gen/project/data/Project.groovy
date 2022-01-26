package org.ods.doc.gen.project.data

import groovy.util.logging.Slf4j
import org.ods.shared.lib.jira.JiraService
import org.springframework.stereotype.Service

import javax.cache.annotation.CacheKey
import javax.cache.annotation.CacheResult
import javax.inject.Inject

@Slf4j
@Service
class Project {

    private final JiraService jira

    @Inject
    Project(JiraService jira){
        this.jira = jira
    }

    @CacheResult(cacheName = "projectData")
    ProjectData getProjectData(@CacheKey String projectBuildId, Map data){
        log.info("build project data for projectBuildId:${projectBuildId}")
        return new ProjectData(jira).init(data).load()
    }

}
