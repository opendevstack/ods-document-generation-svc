package org.ods.doc.gen.core.test.wiremock

/**
 * Add jiraURL in gradle.properties to change default jiraURL
 * Add nexusURL in gradle.properties to change default nexusURL
 * Add docGenURL in gradle.properties to change default docGenURL
 * Add jiraUser & jiraPassword in gradle.properties to change Jira user/password
 */
enum WiremockServers {
    SONAR_QU {
        WiremockManager build() {
            new WiremockManager("sonarQu", System.properties["sonar.url"])
        }
    },
    JIRA {
        WiremockManager build() {
            new WiremockManager("jira", System.properties["jira.url"])
        }
    },
    NEXUS {
        WiremockManager build() {
            new WiremockManager("nexus", System.properties["nexus.url"])
        }
    },
    DOC_GEN {
        WiremockManager build() {
            new WiremockManager("docgen", System.properties["docGen.url"])
        }
    },
    BITBUCKET {
        WiremockManager build() {
            new WiremockManager("bitbucket", System.properties["bitbucket.url"])
        }
    }

    abstract WiremockManager build();
}

