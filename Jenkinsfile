def odsNamespace
def odsGitRef
def odsImageTag
node {
  odsNamespace = env.ODS_NAMESPACE ?: 'ods'
  odsImageTag = env.ODS_IMAGE_TAG ?: 'latest'
  odsGitRef = env.ODS_GIT_REF ?: 'master'
}

library("ods-jenkins-shared-library@${odsGitRef}")

odsComponentPipeline(
  imageStreamTag: "${odsNamespace}/jenkins-agent-maven:${odsImageTag}",
  branchToEnvironmentMapping: [
    '*' : 'dev',
    "${odsGitRef}" : 'test'
  ],
  debug: true
) { context ->
  stageBuild(context)
  odsComponentStageScanWithSonar(context, [branch: '*'])
  odsComponentStageBuildOpenShiftImage(context)
  stageCreatedImageTagLatest(context)
}

def stageBuild(def context) {
  def javaOpts = "-Xmx512m"
  def gradleTestOpts = "-Xmx128m"

  stage('Build and Unit Test') {
    withEnv(["TAGVERSION=${context.tagversion}", "NEXUS_HOST=${context.nexusHost}", "NEXUS_USERNAME=${context.nexusUsername}", "NEXUS_PASSWORD=${context.nexusPassword}", "JAVA_OPTS=${javaOpts}", "GRADLE_TEST_OPTS=${gradleTestOpts}"]) {

      // get wkhtml
      sh (
        script : """
        curl -kLO https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/0.12.4/wkhtmltox-0.12.4_linux-generic-amd64.tar.xz
        tar vxf wkhtmltox-0.12.4_linux-generic-amd64.tar.xz
        mv wkhtmltox/bin/wkhtmlto* /usr/bin
        """,
        label : "get and install wkhtml"
      )

      def status = sh(
        script: "./gradlew clean test shadowJar --stacktrace --no-daemon",
        label : "gradle build",
        returnStatus: true
      )
      if (status != 0) {
        error "Build failed!"
      }

      status = sh(
        script: "cp build/libs/*-all.jar ./docker/app.jar",
        label : "copy resources into docker context",
        returnStatus: true
      )
      if (status != 0) {
        error "Copying failed!"
      }
    }
  }
}

def stageCreatedImageTagLatest(def context) {
	stage('Tag created image') {
		def targetImageTag = context.gitBranch.replace('/','_').replace('-','_')
		sh(
			script: "oc -n ${context.targetProject} tag ${context.componentId}:${context.shortGitCommit} ${context.componentId}:${targetImageTag}",
			label: "Set tag '${targetImageTag}' on is/${context.componentId}"
		)
	}
}
