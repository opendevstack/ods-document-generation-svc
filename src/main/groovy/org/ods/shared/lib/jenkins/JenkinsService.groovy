package org.ods.shared.lib.jenkins

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

import javax.inject.Inject

@Slf4j
@Service
class JenkinsService {

    private static final String XUNIT_SYSTEM_RESULT_DIR = 'build/test-results/test'

    private final IPipelineSteps script

    @Inject
    JenkinsService(IPipelineSteps script) {
        this.script = script
    }

    def stashTestResults(String customXunitResultsDir, String stashNamePostFix = 'stash') {
        def contextresultMap = [:]
        log.info ('Collecting test results, if available ...')
        def xUnitResultDir = XUNIT_SYSTEM_RESULT_DIR
        if (customXunitResultsDir?.trim()?.length() > 0) {
            log.debug "Overwritten testresult location: ${customXunitResultsDir}"
            xUnitResultDir = customXunitResultsDir.trim()
            if (!script.fileExists(xUnitResultDir)) {
                throw new IOException ("Cannot use custom test directory '${xUnitResultDir}' that does not exist!")
            }
        }

        if (!script.fileExists(XUNIT_SYSTEM_RESULT_DIR)) {
            script.sh(script: "mkdir -p ${XUNIT_SYSTEM_RESULT_DIR}", label: "creating test directory")
        }

        if (XUNIT_SYSTEM_RESULT_DIR != xUnitResultDir) {
            log.debug "Copying (applicable) testresults from location: '${xUnitResultDir}' to " +
                " '${XUNIT_SYSTEM_RESULT_DIR}'."
            script.sh(
                script: """
                    ${log.shellScriptDebugFlag}
                    cp -rf ${xUnitResultDir}/* ${XUNIT_SYSTEM_RESULT_DIR} | true
                """,
                label: "Moving test results to system location: ${XUNIT_SYSTEM_RESULT_DIR}"
            )
        }

        def foundTests = 0
        script.dir (XUNIT_SYSTEM_RESULT_DIR) {
            foundTests = script.findFiles(glob: '**/**.xml').size()
        }

        log.debug "Found ${foundTests} test files in '${XUNIT_SYSTEM_RESULT_DIR}'"

        contextresultMap.testResultsFolder = XUNIT_SYSTEM_RESULT_DIR
        contextresultMap.testResults = foundTests

        if (foundTests.toInteger() > 0) {
            script.junit(
                testResults: "${XUNIT_SYSTEM_RESULT_DIR}/**/*.xml",
                allowEmptyResults: true
            )

            def testStashPath = "test-reports-junit-xml-${stashNamePostFix}"
            contextresultMap.xunitTestResultsStashPath = testStashPath
            script.stash(
                name: "${testStashPath}",
                includes: "${XUNIT_SYSTEM_RESULT_DIR}/**/*.xml",
                allowEmpty: true
            )
        } else {
            log.info 'No xUnit results for stashing'
        }

        return contextresultMap
    }

    String getCurrentBuildLogAsHtml () {
        StringWriter writer = new StringWriter()
        this.script.currentBuild.getRawBuild().getLogText().writeHtmlTo(0, writer)
        return writer.getBuffer().toString()
    }

    String getCurrentBuildLogAsText () {
        StringWriter writer = new StringWriter()
        this.script.currentBuild.getRawBuild().getLogText().writeLogTo(0, writer)
        return writer.getBuffer().toString()
    }

    boolean unstashFilesIntoPath(String name, String path, String type) {
        def result = true

        this.script.dir(path) {
            try {
                this.script.unstash(name)
            } catch (e) {
                log.info ("Could not find any files of type '${type}' to unstash for name '${name}'")
                result = false
            }
        }
        return result
    }

    def maybeWithPrivateKeyCredentials(String credentialsId, Closure block) {
        if (privateKeyExists(credentialsId)) {
            script.withCredentials([
                script.sshUserPrivateKey(
                    credentialsId: credentialsId,
                    keyFileVariable: 'PKEY_FILE'
                )
            ]) {
                block(script.env.PKEY_FILE)
            }
        } else {
            block('')
        }
    }

    boolean privateKeyExists(String privateKeyCredentialsId) {
        try {
            script.withCredentials(
                [script.sshUserPrivateKey(credentialsId: privateKeyCredentialsId, keyFileVariable: 'PKEY_FILE')]
            ) {
                true
            }
        } catch (_) {
            false
        }
    }

}
