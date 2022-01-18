package org.ods.doc.gen.leva.doc.services;

class PipelineConfig {
    // TODO: deprecate .pipeline-config.yml in favor of release-manager.yml
    static final List FILE_NAMES = ["release-manager.yml",".pipeline-config.yml"]

    static final String REPO_TYPE_ODS_CODE = "ods"
    static final String REPO_TYPE_ODS_INFRA = "ods-infra"
    static final String REPO_TYPE_ODS_SAAS_SERVICE = "ods-saas-service"
    static final String REPO_TYPE_ODS_SERVICE = "ods-service"
    static final String REPO_TYPE_ODS_TEST = "ods-test"
    static final String REPO_TYPE_ODS_LIB = "ods-library"

    static final String PHASE_EXECUTOR_TYPE_MAKEFILE = "Makefile"
    static final String PHASE_EXECUTOR_TYPE_SHELLSCRIPT = "ShellScript"

    static final List PHASE_EXECUTOR_TYPES = [
    PHASE_EXECUTOR_TYPE_MAKEFILE,
    PHASE_EXECUTOR_TYPE_SHELLSCRIPT
    ]
}
