package org.ods.doc.gen.project.data

import groovy.util.logging.Slf4j
import org.ods.doc.gen.external.modules.git.GitRepoDownloadService
import org.ods.doc.gen.external.modules.jira.JiraService
import org.springframework.stereotype.Service

import javax.cache.annotation.CacheKey
import javax.cache.annotation.CacheResult
import javax.inject.Inject

@Slf4j
@Service
class Project {

    private final JiraService jira
    private final GitRepoDownloadService gitRepoDownloadService

    @Inject
    Project(JiraService jira, GitRepoDownloadService gitRepoDownloadService){
        this.jira = jira
        this.gitRepoDownloadService = gitRepoDownloadService
    }

    @CacheResult(cacheName = "projectData")
    ProjectData getProjectData(@CacheKey String projectBuildId, Map data){
        log.info("build project data for projectBuildId:${projectBuildId}")
        return new ProjectData(jira, gitRepoDownloadService).init(data).load()
    }

}
