package org.ods.doc.gen.leva.doc.fixture

import org.ods.doc.gen.leva.doc.services.Constants

class DocTypeProjectFixturesOverall extends DocTypeProjectFixtureBase {

    DocTypeProjectFixturesOverall() {
        super([ Constants.DocumentType.DTR.toString() ])
    }

    def addDocTypes(Map project, List projects) {
        docTypes.each { docType ->
            if (project.docsToTest.contains(docType))
                projects.add(ProjectFixture.getProjectFixtureBuilder(project, docType).overall(true).build())
        }
    }

}
