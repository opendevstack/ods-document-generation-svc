/* generated jenkins file used for building ODS Document generation service in the prov-dev namespace */
def final projectId = 'prov' // Change if you want to build it elsewhere ...
def final componentId = 'docgen'
def final credentialsId = "${projectId}-cd-cd-user-with-password"
def dockerRegistry
def odsGitRef 
node {
  dockerRegistry = env.DOCKER_REGISTRY
  odsImageTag = env.ODS_IMAGE_TAG ?: 'latest'
  odsGitRef = env.ODS_GIT_REF ?: 'production'
}

library("ods-jenkins-shared-library@${odsGitRef}")

/*
  See readme of shared library for usage and customization
  @ https://github.com/opendevstack/ods-jenkins-shared-library/blob/master/README.md
  eg. to create and set your own builder slave instead of 
  the maven/gradle slave used here - the code of the slave can be found at
  https://github.com/opendevstack/ods-project-quickstarters/tree/master/jenkins-slaves/maven
 */ 
odsPipeline(
  image: "${dockerRegistry}/cd/jenkins-slave-maven:${odsImageTag}",
  projectId: projectId,
  componentId: componentId,
  sonarQubeBranch: "*",
  branchToEnvironmentMapping: [
    '*': 'dev',
	"${odsGitRef}" : 'test'
  ]
) { context ->
  stageBuild(context)
  stageScanForSonarqube(context)  
  stageStartOpenshiftBuild(context)
}

def stageBuild(def context) {
  def javaOpts = "-Xmx512m"
  def gradleTestOpts = "-Xmx128m"

  stage('Build') {
    withEnv(["TAGVERSION=${context.tagversion}", "NEXUS_HOST=${context.nexusHost}", "NEXUS_USERNAME=${context.nexusUsername}", "NEXUS_PASSWORD=${context.nexusPassword}", "JAVA_OPTS=${javaOpts}","GRADLE_TEST_OPTS=${gradleTestOpts}"]) {
	
	  // get wkhtml
      sh "curl -kLO https://downloads.wkhtmltopdf.org/0.12/0.12.4/wkhtmltox-0.12.4_linux-generic-amd64.tar.xz"
      sh "tar vxf wkhtmltox-0.12.4_linux-generic-amd64.tar.xz"
      sh "mv wkhtmltox/bin/wkhtmlto* /usr/bin"
	
      def status = sh(script: "./gradlew clean test shadowJar --stacktrace --no-daemon", returnStatus: true)
      if (status != 0) {
        error "Build failed!"
      }

      status = sh(script: "cp build/libs/*-all.jar ./docker/app.jar", returnStatus: true)
      if (status != 0) {
        error "Copying failed!"
      }
    }
  }
}
