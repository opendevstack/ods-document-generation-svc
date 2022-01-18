package org.ods.shared.lib.orchestration.scheduler



import  org.ods.shared.lib.orchestration.usecase.DocGenUseCase
import org.ods.shared.lib.util.IPipelineSteps
import  org.ods.shared.lib.orchestration.util.MROPipelineUtil
import  org.ods.shared.lib.orchestration.util.Project

// TODO: fix me!
@SuppressWarnings('AbstractClassWithPublicConstructor')
abstract class DocGenScheduler {

    protected Project project
    protected IPipelineSteps steps
    protected MROPipelineUtil util
    protected DocGenUseCase usecase

    DocGenScheduler(Project project, IPipelineSteps steps, MROPipelineUtil util, DocGenUseCase usecase) {
        this.project = project
        this.steps = steps
        this.util = util
        this.usecase = usecase
    }

    
    protected String getMethodNameForDocumentType(String documentType) {
        def name = documentType.replaceAll("OVERALL_", "Overall")
        return "create${name}"
    }

    
    protected int getMethodParamsSizeForDocumentType(String documentType) {
        def name = this.getMethodNameForDocumentType(documentType)
        def method = this.usecase.getMetaClass().getMethods().find { method ->
            return method.isPublic() && method.getName() == name
        }

        return method ? method.getParameterTypes().size() : 0
    }

    protected abstract boolean isDocumentApplicable(
        String documentType,
        String phase,
        MROPipelineUtil.PipelinePhaseLifecycleStage stage,
        Map repo = null)

    void run(String phase, MROPipelineUtil.PipelinePhaseLifecycleStage stage, Map repo = null, Map data = null) {
        def documents = this.usecase.getSupportedDocuments()
        documents.each { documentType ->
            def args = [repo, data]
            def argsDefined = args.findAll()

            def paramsSize = this.getMethodParamsSizeForDocumentType(documentType)
            if (argsDefined.size() > paramsSize) {
                return
            }

            def paramsToApply = paramsSize > 0 ? args[0..(Math.min(args.size(), paramsSize) - 1)] : []

            if (this.isDocumentApplicable(documentType, phase, stage, repo)) {
                def message = "Creating document of type '${documentType}' for project '${this.project.key}'"
                if (repo) {
                    message += " and repo '${repo.id}'"
                }
                this.steps.echo(message)

                // Apply args according to the method's parameters length
                this.usecase.invokeMethod(this.getMethodNameForDocumentType(documentType), paramsToApply as Object[])
            }
        }
    }
}
