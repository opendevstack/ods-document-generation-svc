package org.ods.shared.lib.orchestration.service

import groovy.json.JsonSlurperClassic
import  org.ods.shared.lib.orchestration.service.leva.ProjectDataBitbucketRepository
import  org.ods.shared.lib.orchestration.util.Project
import org.ods.shared.lib.util.IPipelineSteps
import org.ods.shared.lib.util.PipelineSteps
import util.SpecHelper

import static util.FixtureHelper.createProject

class ProjectDataBitbucketRepositorySpec extends SpecHelper {

    Project project
    IPipelineSteps steps

    def setup() {
        project = Spy(createProject())
        project.buildParams.targetEnvironment = "dev"
        project.buildParams.targetEnvironmentToken = "D"
        project.buildParams.version = "WIP"

        steps = Spy(PipelineSteps)
        project.getOpenShiftApiUrl() >> 'https://api.dev-openshift.com'
    }



    def "load content"() {
        given:
        def steps = Spy(util.PipelineSteps)
        def repo = Spy(ProjectDataBitbucketRepository, constructorArgs: [
            steps
        ])
        def version = '1.0'
        def textfile = '{"project": "DEMO"}'
        def jsonObject = new JsonSlurperClassic().parseText(textfile)

        when:
        def result = repo.loadFile(version)

        then:
        1 * steps.readFile(file: "${ProjectDataBitbucketRepository.BASE_DIR}/${version}.json") >> textfile
        result == jsonObject


    }
}
