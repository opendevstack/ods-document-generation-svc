package org.ods.shared.lib.git

import groovy.json.JsonSlurperClassic
import org.ods.shared.lib.jenkins.PipelineSteps
import  org.ods.shared.lib.project.data.Project
import org.ods.shared.lib.jenkins.PipelineSteps
import org.ods.doc.gen.core.test.SpecHelper

import static org.ods.doc.gen.core.test.fixture.FixtureHelper.createProject

class ProjectDataBitbucketRepositorySpec extends SpecHelper {

    Project project
    PipelineSteps steps

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
        def steps = Spy(PipelineSteps)
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
