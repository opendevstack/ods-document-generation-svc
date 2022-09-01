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
  branchToEnvironmentMapping: [:],
  debug: true,
  resourceRequestMemory: '3Gi',
  resourceLimitMemory: '3Gi'
) { context ->
  stageBuild(context)
  odsComponentStageScanWithSonar(context, [branch: '*'])
  odsComponentStageBuildOpenShiftImage(context, [branch: '*'])
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
        curl -sSkLO https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/0.12.4/wkhtmltox-0.12.4_linux-generic-amd64.tar.xz
        tar vxf wkhtmltox-0.12.4_linux-generic-amd64.tar.xz
        mv wkhtmltox/bin/wkhtmlto* /usr/bin

        source use-j11.sh || echo 'ERROR: We could NOT setup jdk 11.'
        ./gradlew --version || echo 'ERROR: Could NOT get gradle version.'
        java -version || echo 'ERROR: Could NOT get java version.'
        echo "JAVA_HOME: $JAVA_HOME" || echo "ERROR: JAVA_HOME has NOT been set."

        """,
        label : "get and install wkhtml"
      )

      def status = sh(
        script: '''
                source use-j11.sh

                retryNum=0
                downloadResult=1
                while [ 0 -ne $downloadResult ] && [ 5 -gt $retryNum ]; do
                    set -x
                    ./gradlew -i dependencies
                    set +x
                    downloadResult=$?
                    let "retryNum=retryNum+1"
                done

                set -x
                ./gradlew -i clean test shadowJar --full-stacktrace --no-daemon
                set +x
        ''',
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
			script: "oc -n ${context.cdProject} tag ${context.componentId}:${context.shortGitCommit} ${context.componentId}:${targetImageTag}",
			label: "Set tag '${targetImageTag}' on is/${context.componentId}"
		)
	}
}
