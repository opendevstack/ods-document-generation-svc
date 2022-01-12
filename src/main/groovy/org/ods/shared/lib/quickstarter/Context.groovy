package org.ods.shared.lib.quickstarter



class Context implements IContext {

    private final Map config

    Context(Map config) {
        this.config = config
    }

    
    String getJobName() {
        config.jobName
    }

    
    String getBuildNumber() {
        config.buildNumber
    }

    
    String getBuildUrl() {
        config.buildUrl
    }

    
    String getBuildTime() {
        config.buildTime
    }

    
    String getDockerRegistry() {
        config.dockerRegistry
    }

    
    String getProjectId() {
        config.projectId
    }

    
    String getComponentId() {
        config.componentId
    }

    
    String getSourceDir() {
        config.sourceDir
    }

    
    String getGitUrlHttp() {
        config.gitUrlHttp
    }

    
    String getOdsNamespace() {
        config.odsNamespace
    }

    
    String getOdsImageTag() {
        config.odsImageTag
    }

    
    String getOdsGitRef() {
        config.odsGitRef
    }

    
    String getAgentImageTag() {
        config.agentImageTag
    }

    
    String getSharedLibraryRef() {
        config.sharedLibraryRef
    }

    
    String getTargetDir() {
        config.targetDir
    }

    
    String getPackageName() {
        config.packageName
    }

    
    String getGroup() {
        config.group
    }

    
    String getCdUserCredentialsId() {
        config.cdUserCredentialsId
    }

    
    String getBitbucketUrl() {
        config.bitbucketUrl
    }

    
    String getBitbucketHost() {
        getBitbucketUrl()
    }

    
    String getBitbucketHostWithoutScheme() {
        getBitbucketUrl().minus(~/^https?:\/\//)
    }

    
    String getNexusUrl() {
        config.nexusUrl
    }

    
    String getNexusHost() {
        getNexusUrl()
    }

    
    String getNexusHostWithoutScheme() {
        getNexusUrl().minus(~/^https?:\/\//)
    }

    
    String getNexusUsername() {
        config.nexusUsername
    }

    
    String getNexusPassword() {
        config.nexusPassword
    }

    
    String getGitBranch() {
        config.odsGitRef
    }

}
