package org.ods.doc.gen.leva.doc.fixture

class DocTypeProjectFixture extends DocTypeProjectFixtureBase {

    DocTypeProjectFixture() {
        super(["CSD", "DIL", "DTP", "RA", "CFTP", "IVP", "SSDS", "TCP", "TIP", "TRC"])
    }

    DocTypeProjectFixture(docTypes) {
        super(docTypes)
    }

    def addDocTypes(Map project, List projects) {
        docTypes.each { docType ->
            if (project.docsToTest.contains(docType))
                projects.add(ProjectFixture.getProjectFixtureBuilder(project, docType as String).build())
        }
    }

}
