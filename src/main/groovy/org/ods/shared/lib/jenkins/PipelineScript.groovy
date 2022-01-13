package org.ods.shared.lib.jenkins

import groovy.util.logging.Slf4j
import org.ods.shared.lib.jenkins.IPipelineSteps
import org.springframework.stereotype.Service

// We're stubbing out a pipeline script.
@Slf4j
@Service
class PipelineScript implements  IPipelineSteps {

    Map getEnv(){
        return  [
                'BUILD_ID'   : 15,
                'BRANCH_NAME': 'PR-10',
                'JOB_NAME': 'foo-cd/foo-cd-JOB-10',
                'BUILD_NUMBER': '1',
                'BUILD_TAG': 'foo',
                'NEXUS_HOST': 'https//nexus.example.com',
                'NEXUS_USERNAME': 'foo',
                'NEXUS_PASSWORD': 'bar',
                'OPENSHIFT_API_URL': 'https://api.openshift.example.com',
                'BITBUCKET_HOST': 'bitbucket.example.com',
                "getEnvironment" : { [ : ] }
        ]
    }

    @Override
    void junit(String path) {

    }

    @Override
    void junit(Map config) {

    }

    @Override
    def load(String path) {
        return null
    }
    def scm

    def currentBuild = [:]

    def USERPASS = 'secret'

    def node(String label, Closure body) {
        body()
    }

    void stage(String label, Closure body) {
        body()
    }

    def wrap(def foo, Closure body) {
        body()
    }

    def withCredentials(def foo, Closure body) {
        body()
    }

    def sh(def foo) {
        return "test"
    }

    def emailextrecipients(def foo) {
    }

    def emailext(def foo) {
    }

    def checkout(def foo) {
    }

    def usernameColonPassword(def foo) {
    }

    def PipelineScript() {
    }

    def error(msg) {
        println msg
    }

    def echo(msg) {
        println msg
    }

    def fileExists (file) {
        return true
    }

    def zip (Map cfg) {

    }

    def parallel (Map executors) {
        if (executors) {
            executors.remove ('failFast')
            executors.each { key, block ->
                println key
                block ()
            }
        }
    }

    @Override
    void archiveArtifacts(String artifacts) {

    }

    @Override
    void archiveArtifacts(Map args) {

    }

    @Override
    def checkout(Map config) {
        return null
    }

    @Override
    void dir(String path, Closure block) {

    }

    @Override
    void echo(String message) {

    }

    @Override
    void error(String message) {

    }

    @Override
    void stash(String name) {

    }

    @Override
    void stash (Map args) {

    }

    @Override
    void unstash(String name) {

    }

    @Override
    def fileExists(String file) {
        return null
    }

    @Override
    def readFile(String file, String encoding) {
        return null
    }

    def readFile (Map args) {

    }

    @Override
    def writeFile(String file, String text, String encoding) {
        return null
    }

    @Override
    def writeFile(Map args) {
        return null
    }

    def readJSON (Map args) {

    }

    @Override
    def writeJSON(Map args) {
        return null
    }

    @Override
    def readYaml(Map args) {
        return null
    }

    @Override
    def writeYaml(Map args) {
        return null
    }

    @Override
    def timeout(Map args, Closure block) {
        return null
    }

    @Override
    def deleteDir() {
        return null
    }

    @Override
    def sleep(int seconds) {
        return null
    }

    @Override
    def withEnv(List<String> env, Closure block) {
        return null
    }

    @Override
    def unstable(String message) {
        return null
    }

    @Override
    def usernamePassword(Map credentialsData) {
        return null
    }

    @Override
    def sshUserPrivateKey(Map credentialsData) {
        return null
    }

    @Override
    def withCredentials(List credentialsList, Closure block) {
        return null
    }

    @Override
    def unwrap() {
        return null
    }

    @Override
    def emailext(Map args) {
        return null
    }

    def withSonarQubeEnv(String conf, Closure closure) {
        closure()
    }

    def SONAR_HOST_URL = "https://sonarqube.example.com"

    def SONAR_AUTH_TOKEN = "Token"
}
