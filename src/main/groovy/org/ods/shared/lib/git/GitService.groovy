package org.ods.shared.lib.git

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@SuppressWarnings('MethodCount')
@Slf4j
@Service
class GitService {

    @SuppressWarnings('NonFinalPublicField')
    public static String ODS_GIT_TAG_PREFIX = 'ods-generated-'
    public final static String ODS_GIT_BRANCH_PREFIX = 'release/'

    private final def script



    static String getReleaseBranch(String version) {
        "${ODS_GIT_BRANCH_PREFIX}${version}"
    }

    void checkout(
        String branch,
        def extensions,
        def userRemoteConfigs,
        boolean doGenerateSubmoduleConfigurations = false) {
        def branches = [[name: branch]]
        this.checkout(
            branches,
            extensions,
            userRemoteConfigs,
            doGenerateSubmoduleConfigurations
        )
        }

    void checkout(
        def branches,
        def extensions,
        def userRemoteConfigs,
        boolean doGenerateSubmoduleConfigurations = false) {
        def gitParams = [
            $class: 'GitSCM',
            branches: branches,
            doGenerateSubmoduleConfigurations: doGenerateSubmoduleConfigurations,
            extensions: [[
                    $class: 'SubmoduleOption',
                    disableSubmodules: false,
                    parentCredentials: true,
                    recursiveSubmodules: true,
                    reference: '',
                    trackingSubmodules: false],
                    [$class: 'CleanBeforeCheckout'],
                    [$class: 'CleanCheckout']
                    ],
            submoduleCfg: [],
            userRemoteConfigs: userRemoteConfigs,
        ]
        if (!extensions.empty) {
            gitParams.extensions += extensions
        }
        if (isAgentNodeGitLfsEnabled()) {
            gitParams.extensions << [$class: 'GitLFSPull']
        }
        // script.checkout(gitParams) // s2o removed script
    }



    def commit(List files, String msg, boolean allowEmpty = true) {
        def allowEmptyFlag = allowEmpty ? '--allow-empty' : ''
        def filesToAddCommand = "git add ${files.join(' ')}"
        if (files.empty) {
            filesToAddCommand = ''
        }
        script.sh(
            script: """
                ${filesToAddCommand}
                git commit -m "${msg}" ${allowEmptyFlag}
            """,
            label: 'Commit'
        )
    }



    private boolean isAgentNodeGitLfsEnabled() {
        def statusCode = script.sh(
            script: 'git lfs &> /dev/null',
            label: 'Check if Git LFS is enabled',
            returnStatus: true
        )
        return statusCode == 0
    }

}
