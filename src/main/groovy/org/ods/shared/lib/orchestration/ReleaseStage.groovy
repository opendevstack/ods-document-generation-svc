package org.ods.shared.lib.orchestration

import org.ods.shared.lib.services.ServiceRegistry
import  org.ods.shared.lib.orchestration.scheduler.LeVADocumentScheduler
import  org.ods.shared.lib.orchestration.util.MROPipelineUtil
import  org.ods.shared.lib.orchestration.util.Project

class ReleaseStage extends Stage {

    public final String STAGE_NAME = 'Release'

    ReleaseStage(def script, Project project, List<Set<Map>> repos) {
        super(script, project, repos)
    }

    @SuppressWarnings('ParameterName')
    def run() {
        def levaDocScheduler = ServiceRegistry.instance.get(LeVADocumentScheduler)
        def util = ServiceRegistry.instance.get(MROPipelineUtil)

        def phase = MROPipelineUtil.PipelinePhases.RELEASE

        def preExecuteRepo = { steps_, repo ->
            levaDocScheduler.run(phase, MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_EXECUTE_REPO, repo)
        }

        def postExecuteRepo = { steps_, repo ->
            levaDocScheduler.run(phase, MROPipelineUtil.PipelinePhaseLifecycleStage.POST_EXECUTE_REPO, repo)
        }

        Closure generateDocuments = {
            levaDocScheduler.run(phase, MROPipelineUtil.PipelinePhaseLifecycleStage.POST_START)
        }

        // Execute phase for each repository
        Closure executeRepos = {
            util.prepareExecutePhaseForReposNamedJob(phase, repos, preExecuteRepo, postExecuteRepo)
                .each { group ->
                    group.failFast = true
                    script.parallel(group)
                }
        }
        executeInParallel(executeRepos, generateDocuments)

        levaDocScheduler.run(phase, MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END)
    }

}
