package org.ods.shared.lib.orchestration.usecase

import org.ods.shared.lib.util.IPipelineSteps
import  org.ods.shared.lib.orchestration.util.Project

@SuppressWarnings('AbstractClassWithPublicConstructor')
abstract class AbstractJiraUseCaseSupport {

    protected Project project
    protected IPipelineSteps steps
    protected JiraUseCase usecase

    AbstractJiraUseCaseSupport(Project project, IPipelineSteps steps, JiraUseCase usecase) {
        this.project = project
        this.steps = steps
        this.usecase = usecase
    }

    abstract void applyXunitTestResults(List testIssues, Map testResults)
}
