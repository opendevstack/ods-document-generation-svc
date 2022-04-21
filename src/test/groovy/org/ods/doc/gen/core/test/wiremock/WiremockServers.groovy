package org.ods.doc.gen.core.test.wiremock

/**
 * Add jiraURL in gradle.properties to change default jiraURL
 * Add nexusURL in gradle.properties to change default nexusURL
 * Add docGenURL in gradle.properties to change default docGenURL
 * Add jiraUser & jiraPassword in gradle.properties to change Jira user/password
 */
enum WiremockServers {
    SONAR_QU {
        WiremockManager build(String url) {
            new WiremockManager("sonarQu", url)
        }
    },
    JIRA {
        WiremockManager build(String url) {
            new WiremockManager("jira", url)
        }
    },
    NEXUS {
        WiremockManager build(String url) {
            new WiremockManager("nexus", url)
        }
    },
    BITBUCKET {
        WiremockManager build(String url) {
            new WiremockManager("bitbucket", url)
        }
    },
    GITHUB {
        WiremockManager build(String url) {
            new WiremockManager("github", url)
        }
    }

    abstract WiremockManager build(String url);
}

