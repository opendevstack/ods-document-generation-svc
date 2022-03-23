package org.ods.doc.gen.leva.doc.fixture

import org.yaml.snakeyaml.Yaml

abstract class DocTypeProjectFixtureBase {

    public static final String DUMMY_PROJECT = "dummyProject"

    private static final String FILENAME_WITH_PROJECTS = "leva-doc-functional-test-projects.yml"
    private static final String FILENAME_PATH = "src/test/resources"

    private static final String LEVA_DOC_FUNCTIONAL_TESTS_PROJECTS = "${FILENAME_PATH}/${FILENAME_WITH_PROJECTS}"
    private static final String ONLY_TEST_ONE_PROJECT = "ordgp"
    private static final List<String> SKIP_TEST_PROJECTS = [ "twrfdgp", "trdgp", "g3dgp", "g4dgp", "brassp"]

    protected final List docTypes

    DocTypeProjectFixtureBase(docTypes){
        this.docTypes = docTypes
    }

    List<ProjectFixture> getProjects(){
        List projectsToTest = []
        try {
            def functionalTest = new Yaml().load(new File(LEVA_DOC_FUNCTIONAL_TESTS_PROJECTS).text)
            List projects = functionalTest.projects.findAll { project ->
                if (ONLY_TEST_ONE_PROJECT == project.id)
                    return true
                if (ONLY_TEST_ONE_PROJECT.length()>0)
                    return false
                if (SKIP_TEST_PROJECTS.size()>0 && SKIP_TEST_PROJECTS.contains(project.id))
                    return false
                return true
            }
            projects.each{ project ->
                addDocTypes(project as Map, projectsToTest)
            }

            if (projectsToTest.isEmpty()){
                projectsToTest.add(
                        ProjectFixture
                                .getProjectFixtureBuilder(buildDummyProject(), "No DocTpe match")
                                .component("No DocTpe defined")
                                .build())
            }
        } catch(Throwable runtimeException){
            // If there's an error here, the log.error doesn't work
            // neither the print
            // So, if there's an error in the startup, put a debug line here
            throw runtimeException
        }

        return projectsToTest
    }

    abstract addDocTypes(Map project, List projects)

    private Map buildDummyProject(){
        return [
                id: DUMMY_PROJECT,
                description: """
                    This project is created when there's no DocType related to the test to execute.
                    As we filter the DocTypeProjectFixture* with the ones defined here
                    leva-doc-functional-test-projects.yml --> docsToTest
                    """
        ]
    }
}
