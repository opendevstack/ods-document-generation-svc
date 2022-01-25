package org.ods.shared.lib.project.data

import groovy.util.logging.Slf4j
import org.ods.shared.lib.git.GitService
import org.ods.shared.lib.jira.JiraService
import org.springframework.stereotype.Service

import javax.cache.annotation.CacheKey
import javax.cache.annotation.CacheResult
import javax.inject.Inject

@Slf4j
@Service
class Project {

    private final GitService gitService
    private final JiraService jira

    @Inject
    Project(GitService gitService, JiraService jira){
        this.gitService = gitService
        this.jira = jira
    }

    @CacheResult(cacheName = "projectData")
    ProjectData getProjectData(@CacheKey String projectBuildId, Map data){
        log.info("build project data for projectBuildId:${projectBuildId}")
        return new ProjectData(jira, gitService).init(data).load()
    }

}
