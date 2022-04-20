package org.ods.doc.gen.project.data

class RepoMetadata {

    private Map privateMetadata

    RepoMetadata(Map metadata) {
        this.privateMetadata = metadata
    }

    String getId() {
        return privateMetadata.id
    }

    String getName() {
        return privateMetadata.name
    }

    String getDescription() {
        return privateMetadata.description
    }

    String getSupplier() {
        return privateMetadata.supplier
    }

    String getVersion() {
        return privateMetadata.version
    }

    String getReferences() {
        return privateMetadata.references
    }

    String getGitUrl() {
        return privateMetadata.gitUrl
    }
}
