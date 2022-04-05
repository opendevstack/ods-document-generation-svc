package org.ods.doc.gen.project.data

import groovy.util.logging.Slf4j
import org.ods.doc.gen.external.modules.git.BitbucketService
import org.ods.doc.gen.external.modules.jira.JiraService
import org.ods.doc.gen.external.modules.xunit.JUnitReportsService
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

import javax.inject.Inject

@Slf4j
@Service
class Project {

    private final JiraService jira
    private final BitbucketService bitbucketService
    private final JUnitReportsService jUnitReportsService

    @Inject
    Project(JiraService jira, BitbucketService bitbucketService, JUnitReportsService jUnitReportsService){
        this.jira = jira
        this.bitbucketService = bitbucketService
        this.jUnitReportsService = jUnitReportsService
    }

    @Cacheable(value = "projectData", key = '#projectBuildId')
    ProjectData getProjectData(String projectBuildId, Map data){
        log.info("build project data for projectBuildId:${projectBuildId}")
        return new ProjectData(jira, bitbucketService, jUnitReportsService).init(data).load()
    }

}
