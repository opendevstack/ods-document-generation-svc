![ODS Document Generation Service tests](https://github.com/opendevstack/ods-document-generation-svc/workflows/Document%20Generation%20Service/badge.svg)

# ODS Document Generation Service

A document generation service that:
- Transforms document templates in a remote Bitbucket repository into PDF documents.
- Generate LeVA documentation and uploads them to a Nexus service

## Distribute
In order to generate a distributed app a Docker client needs to be configured.
- The _gradle_ task: __dockerBuildImage__ generates a Docker image with the application

### Environment
The file src/main/resources/application.yml has the properties and parameters that the app admit.
 
## Unit Tests
In order to execute the tests a Docker client needs to be configured.

```
gradle test
```

## Integration Tests
In order to execute the tests a Docker client needs to be configured.

```
gradle dockerTest
```

## Performance Tests
In order to execute the tests a Docker client needs to be configured.

```
gradle gatlingRun
```

## Document Templates
When processing a template `type` at a specific `version`, and data into a document, the DocGen service expects the BitBucket repository to have a `release/${version}` branch that contains the template type at `/templates/${type}.html.tmpl`.

## LeVA Doc generation
When generating LeVA documentation, the app expects the xml tests results and the Jenkins Logs to be in a Nexus service.

## Structure package
Main groovy code in src/main/groovy:
- org.ods.doc.gen.core:  common 'utilities'
- org.ods.doc.gen.adapters: clients to other systems. They translate the interfaces of external systems to the interfaces required.
- org.ods.doc.gen.doc: LeVA document generation functional module
- org.ods.doc.pdf.builder: PDF generation document functional module


## History
The module __"leva.doc"__ is a refactor extracted from the LevaDoc functionality of the [shared-library](https://github.com/opendevstack/ods-jenkins-shared-library).
You can see the shared-library before the refactor here: https://github.com/opendevstack/ods-jenkins-shared-library/tree/feature/beforeMoveLevaDoc

Some correspondences between the shared-lib classes and this project:
- org.ods.orchestration.util.Project => org.ods.doc.gen.project.data.ProjectData
- org.ods.orchestration.usecase.LeVADocumentUseCase => org.ods.doc.gen.leva.doc.services.LeVADocumentService
- org.ods.orchestration.usecase.DocGenUseCase => org.ods.doc.gen.leva.doc.services.DocGenService
- org.ods.orchestration.util.DocumentHistory => org.ods.doc.gen.leva.doc.services.DocumentHistory
- org.ods.orchestration.usecase.PDFUtil => org.ods.doc.gen.leva.doc.services.PDFService


