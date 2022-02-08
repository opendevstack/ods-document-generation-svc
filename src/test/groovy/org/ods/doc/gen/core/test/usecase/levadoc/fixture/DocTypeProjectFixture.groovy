package org.ods.doc.gen.core.test.usecase.levadoc.fixture

class DocTypeProjectFixture extends DocTypeProjectFixtureBase {

    DocTypeProjectFixture() {
     //   super(["CSD", "DIL", "DTP", "RA", "CFTP", "IVP", "SSDS", "TCP", "TIP", "TRC"])
        super([ "TRC"])
    }

    DocTypeProjectFixture(docTypes) {
        super(docTypes)
    }

    def addDocTypes(Map project, List projects) {
        docTypes.each { docType ->
            projects.add(ProjectFixture.getProjectFixtureBuilder(project, docType as String).build())
        }
    }

}
