package org.ods.doc.gen.project.data

import org.ods.doc.gen.leva.doc.services.PipelineConfig

class RepoData {

    Map privateRepoData
    RepoMetadata privateRepoMetadata

    RepoData(Map repoData, RepoMetadata repoMetadata) {

        // Check for existence of required attribute 'repositories[i].id'
        if (!repoData.id?.trim()) {
            throw new IllegalArgumentException(
                    "Error: unable to parse project meta data. Required attribute 'repositories[${index}].id' is undefined.")
        }

        /*
        repoData.data = [
                openshift: [:],
                documents: [:],
        ]
        */

        // Set repo type, if not provided
        if (!repoData.type?.trim()) {
            repoData.type = PipelineConfig.REPO_TYPE_ODS_CODE
        }

        this.privateRepoData = repoData
        this.privateRepoMetadata = repoMetadata
    }

    RepoMetadata getMetadata() {
        return this.privateRepoMetadata
    }

    String getId() {
        return privateRepoData.id
    }

    String getName() {
        return privateRepoData.name
    }

    String getType() {
        return privateRepoData.type
    }

    String getBranch() {
        return privateRepoData.branch
    }

    String getDoInstall() {
        return privateRepoData.doInstall
    }

    void setDoInstall(String doInstall) {
        privateRepoData.doInstall = doInstall
    }
}
