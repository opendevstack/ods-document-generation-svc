package org.ods.doc.gen.leva.doc.services

import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.external.modules.git.BitbucketTraceabilityUseCase
import org.ods.doc.gen.external.modules.jenkins.JenkinsService
import org.ods.doc.gen.external.modules.jira.CustomIssueFields
import org.ods.doc.gen.external.modules.jira.IssueTypes
import org.ods.doc.gen.external.modules.jira.JiraUseCase
import org.ods.doc.gen.external.modules.jira.LabelPrefix
import org.ods.doc.gen.external.modules.nexus.NexusService
import org.ods.doc.gen.external.modules.sonar.SonarQubeUseCase
import org.ods.doc.gen.external.modules.xunit.JUnitTestReportsUseCase
import org.ods.doc.gen.project.data.Environment
import org.ods.doc.gen.project.data.Project
import org.ods.doc.gen.project.data.ProjectData
import org.ods.doc.gen.project.data.TestType
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.time.Clock
import java.time.LocalDateTime

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson

@SuppressWarnings([
    'ClassSize',
    'UnnecessaryDefInMethodDeclaration',
    'UnnecessaryCollectCall',
    'IfStatementBraces',
    'LineLength',
    'AbcMetric',
    'Instanceof',
    'VariableName',
    'DuplicateListLiteral',
    'UnusedMethodParameter',
    'UnusedVariable',
    'ParameterCount',
    'ParameterReassignment',
    'UnnecessaryElseStatement',
    'NonFinalPublicField',
    'PropertyName',
    'MethodCount',
    'UseCollectMany',
    'ParameterName',
    'TrailingComma',
    'SpaceAroundMapEntryColon',
    'PublicMethodsBeforeNonPublicMethods'])
@Slf4j
@Service
class LeVADocumentService extends DocGenUseCase {

    static final String SONARQUBE_BASE_DIR = 'sonarqube'

    private final JiraUseCase jiraUseCase
    private final JUnitTestReportsUseCase junit
    private final LeVADocumentChaptersFileService levaFiles
    private final SonarQubeUseCase sq
    private final BitbucketTraceabilityUseCase bbt

    @Inject
    Clock clock

    @Inject
    LeVADocumentService(Project project, ZipFacade util, DocGenService docGen,
                        JenkinsService jenkins, JiraUseCase jiraUseCase, JUnitTestReportsUseCase junit,
                        LeVADocumentChaptersFileService levaFiles, NexusService nexus,
                        PDFUtil pdf, SonarQubeUseCase sq, BitbucketTraceabilityUseCase bbt) {
        super(project, util, docGen, nexus, pdf, jenkins)
        this.jiraUseCase = jiraUseCase
        this.junit = junit
        this.levaFiles = levaFiles
        this.sq = sq
        this.bbt = bbt
    }

    @SuppressWarnings('CyclomaticComplexity')
    String createCSD(Map data) {
        log.info("createCSD for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        String documentType = data.documentType as String

        def sections = this.getDocumentSections(documentType, projectData)
        def watermarkText = this.getWatermarkText(projectData)

        def requirements = projectData.getSystemRequirements()
        def reqsWithNoGampTopic = getReqsWithNoGampTopic(requirements)
        def reqsGroupedByGampTopic = getReqsGroupedByGampTopic(requirements)
        reqsGroupedByGampTopic << ['uncategorized': reqsWithNoGampTopic]

        def requirementsForDocument = reqsGroupedByGampTopic.collectEntries { gampTopic, reqs ->
            def updatedReqs = reqs.collect { req ->
                def epics = req.getResolvedEpics()
                def epic = epics.isEmpty() ? null : epics.first()
                return [
                    key             : req.key,
                    applicability   : 'Mandatory',
                    ursName         : req.name,
                    ursDescription  : this.convertImages(req.description ?: ''),
                    csName          : req.configSpec.name ?: 'N/A',
                    csDescription   : this.convertImages(req.configSpec.description ?: ''),
                    fsName          : req.funcSpec.name ?: 'N/A',
                    fsDescription   : this.convertImages(req.funcSpec.description ?: ''),
                    epic            : epic?.key,
                    epicName        : epic?.epicName,
                    epicTitle       : epic?.title,
                    epicDescription : this.convertImages(epic?.description),
                ]
            }

            def output = sortByEpicAndRequirementKeys(updatedReqs)

            return [
                (gampTopic.replaceAll(' ', '').toLowerCase()): output
            ]
        }

        List<String> keysInDoc = computeKeysInDocForCSD(projectData.getRequirements())
        if (projectData.data?.jira?.discontinuationsPerType) {
            keysInDoc += projectData.data.jira.discontinuationsPerType.requirements*.key
            keysInDoc += projectData.data.jira.discontinuationsPerType.epics*.key
        }

        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)
        def data_ = [
            metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
            data    : [
                sections    : sections,
                requirements: requirementsForDocument,
                documentHistory: docHistory?.getDocGenFormat() ?: []
            ]
        ]

        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, 
                getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    String createDIL(Map data) {
        log.info("createDIL for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.DIL as String

        def watermarkText = this.getWatermarkText(projectData)

        def bugs = projectData.getBugs().each { bug ->
            bug.tests = bug.getResolvedTests()
        }

        def acceptanceTestBugs = bugs.findAll { bug ->
            bug.tests.findAll { test ->
                test.testType == TestType.ACCEPTANCE
            }
        }

        def integrationTestBugs = bugs.findAll { bug ->
            bug.tests.findAll { test ->
                test.testType == TestType.INTEGRATION
            }
        }

        SortUtil.sortIssuesByKey(acceptanceTestBugs)
        SortUtil.sortIssuesByKey(integrationTestBugs)

        Map metadata = this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType] as String)
        metadata.orientation = "Landscape"

        Map data_ = [
                metadata: metadata,
                data    : [:]
        ]

        if (!integrationTestBugs.isEmpty()) {
            data_.data.integrationTests = integrationTestBugs.collect { bug ->
                [
                        //Discrepancy ID -> BUG Issue ID
                        discrepancyID        : bug.key,
                        //Test Case No. -> JIRA (Test Case Key)
                        testcaseID           : bug.tests. collect { it.key }.join(", "),
                        //- Level of Test Case = Unit / Integration / Acceptance / Installation
                        level                : "Integration",
                        //Description of Failure or Discrepancy -> Bug Issue Summary
                        description          : bug.name,
                        //Remediation Action -> "To be fixed"
                        remediation          : "To be fixed",
                        //Responsible / Due Date -> JIRA (assignee, Due date)
                        responsibleAndDueDate: "${bug.assignee ? bug.assignee : 'N/A'} / ${bug.dueDate ? bug.dueDate : 'N/A'}",
                        //Outcome of the Resolution -> Bug Status
                        outcomeResolution    : bug.status,
                        //Resolved Y/N -> JIRA Status -> Done = Yes
                        resolved             : bug.status == "Done" ? "Yes" : "No"
                ]
            }
        }

        if (!acceptanceTestBugs.isEmpty()) {
            data_.data.acceptanceTests = acceptanceTestBugs.collect { bug ->
                [
                        //Discrepancy ID -> BUG Issue ID
                        discrepancyID        : bug.key,
                        //Test Case No. -> JIRA (Test Case Key)
                        testcaseID           : bug.tests. collect { it.key }.join(", "),
                        //- Level of Test Case = Unit / Integration / Acceptance / Installation
                        level                : "Acceptance",
                        //Description of Failure or Discrepancy -> Bug Issue Summary
                        description          : bug.name,
                        //Remediation Action -> "To be fixed"
                        remediation          : "To be fixed",
                        //Responsible / Due Date -> JIRA (assignee, Due date)
                        responsibleAndDueDate: "${bug.assignee ? bug.assignee : 'N/A'} / ${bug.dueDate ? bug.dueDate : 'N/A'}",
                        //Outcome of the Resolution -> Bug Status
                        outcomeResolution    : bug.status,
                        //Resolved Y/N -> JIRA Status -> Done = Yes
                        resolved             : bug.status == "Done" ? "Yes" : "No"
                ]
            }
        }

        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri)
        return uri
    }

    String createDTP(Map data) {
        log.info("createDTP for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.DTP as String

        def sections = this.getDocumentSectionsFileOptional(projectData, documentType)
        def watermarkText = this.getWatermarkText(projectData)

        def unitTests = projectData.getAutomatedTestsTypeUnit()
        def tests = this.computeTestsWithRequirementsAndSpecs(projectData, unitTests)
        def modules = this.getReposWithUnitTestsInfo(projectData, unitTests)

        def keysInDoc = this.computeKeysInDocForDTP(modules, tests)
        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)

        def data_ = [
            metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
            data    : [
                sections: sections,
                tests: tests,
                modules: modules,
                documentHistory: docHistory?.getDocGenFormat() ?: [],
            ]
        ]

        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    String createRA(Map data) {
        log.info("createRA for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.RA as String

        def sections = this.getDocumentSections(documentType, projectData)
        def watermarkText = this.getWatermarkText(projectData)

        def obtainEnum = { category, value ->
            return projectData.getEnumDictionary(category)[value as String]
        }

        def risks = projectData.getRisks()
                .findAll {  it != null }
                .collect { r ->
                    def mitigationsText = this.replaceDashToNonBreakableUnicode(r.mitigations ? r.mitigations.join(", ") : "None")
                    def testsText = this.replaceDashToNonBreakableUnicode(r.tests ? r.tests.join(", ") : "None")
                    def requirements = (r.getResolvedSystemRequirements() + r.getResolvedTechnicalSpecifications())
                    def gxpRelevance = obtainEnum("GxPRelevance", r.gxpRelevance)
                    def probabilityOfOccurrence = obtainEnum("ProbabilityOfOccurrence", r.probabilityOfOccurrence)
                    def severityOfImpact = obtainEnum("SeverityOfImpact", r.severityOfImpact)
                    def probabilityOfDetection = obtainEnum("ProbabilityOfDetection", r.probabilityOfDetection)
                    def riskPriority = obtainEnum("RiskPriority", r.riskPriority)

                    return [
                            key: r.key,
                            name: r.name,
                            description: convertImages(r.description),
                            proposedMeasures: "Mitigations: ${ mitigationsText }<br/>Tests: ${ testsText }",
                            requirements: requirements.findAll { it != null }.collect { it.name }.join("<br/>"),
                            requirementsKey: requirements.findAll { it != null }.collect { it.key }.join("<br/>"),
                            gxpRelevance: gxpRelevance ? gxpRelevance."short" : "None",
                            probabilityOfOccurrence: probabilityOfOccurrence ? probabilityOfOccurrence."short" : "None",
                            severityOfImpact: severityOfImpact ? severityOfImpact."short" : "None",
                            probabilityOfDetection: probabilityOfDetection ? probabilityOfDetection."short" : "None",
                            riskPriority: riskPriority ? riskPriority.value : "N/A",
                            riskPriorityNumber: r.riskPriorityNumber ?: "N/A",
                            riskComment: r.riskComment ? r.riskComment : "N/A",
                    ]
                }

        def proposedMeasuresDesription = projectData.getRisks().collect { r ->
            (r.getResolvedTests().collect {
                if (!it) throw new IllegalArgumentException("Error: test for requirement ${r.key} could not be obtained. Check if all of ${r.tests.join(", ")} exist in JIRA")
                [key: it.key, name: it.name, description: it.description, type: "test", referencesRisk: r.key]
            } + r.getResolvedMitigations().collect { [key: it.key, name: it.name, description: it.description, type: "mitigation", referencesRisk: r.key] })
        }.flatten()

        if (!sections."sec4s2s1") sections."sec4s2s1" = [:]
        sections."sec4s2s1".nonGxpEvaluation = projectData.getProjectProperties()."PROJECT.NON-GXP_EVALUATION" ?: 'n/a'

        if (!sections."sec4s2s2") sections."sec4s2s2" = [:]

        if (projectData.getProjectProperties()."PROJECT.USES_POO" == "true") {
            sections."sec4s2s2" = [
                    usesPoo          : "true",
                    lowDescription   : projectData.getProjectProperties()."PROJECT.POO_CAT.LOW",
                    mediumDescription: projectData.getProjectProperties()."PROJECT.POO_CAT.MEDIUM",
                    highDescription  : projectData.getProjectProperties()."PROJECT.POO_CAT.HIGH"
            ]
        }

        if (!sections."sec5") sections."sec5" = [:]
        sections."sec5".risks = SortUtil.sortIssuesByProperties(risks, ["requirementsKey", "key"])
        sections."sec5".proposedMeasures = SortUtil.sortIssuesByKey(proposedMeasuresDesription)

        def metadata = this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType])
        metadata.orientation = "Landscape"

        def keysInDoc = this.computeKeysInDocForRA(projectData.getRisks())
        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)

        def data_ = [
                metadata: metadata,
                data    : [
                        sections: sections,
                        documentHistory: docHistory?.getDocGenFormat() ?: [],
                ]
        ]

        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    String createCFTP(Map data) {
        log.info("createCFTP for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.CFTP as String

        def sections = this.getDocumentSections(documentType, projectData)
        def watermarkText = this.getWatermarkText(projectData)

        def keysInDoc = []
        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)

        def data_ = [
                metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
                data    : [
                        sections        : sections,
                        documentHistory: docHistory?.getDocGenFormat() ?: [],
                ]
        ]

        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    String createIVP(Map data) {
        log.info("createIVP for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.IVP as String

        def sections = this.getDocumentSections(documentType, projectData)
        def watermarkText = this.getWatermarkText(projectData)

        def installationTestIssues = projectData.getAutomatedTestsTypeInstallation()

        def testsGroupedByRepoType = groupTestsByRepoType(installationTestIssues)

        def testsOfRepoTypeOdsCode = []
        def testsOfRepoTypeOdsService = []
        testsGroupedByRepoType.each { repoTypes, tests ->
            if (repoTypes.contains(PipelineConfig.REPO_TYPE_ODS_CODE)) {
                testsOfRepoTypeOdsCode.addAll(tests)
            }

            if (repoTypes.contains(PipelineConfig.REPO_TYPE_ODS_SERVICE)) {
                testsOfRepoTypeOdsService.addAll(tests)
            }
        }

        def keysInDoc = this.computeKeysInDocForIPV(installationTestIssues)
        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)

        def installedRepos = projectData.repositories.collect {
            it << [ doInstall: !Constants.COMPONENT_TYPE_IS_NOT_INSTALLED.contains(it.type?.toLowerCase())]
        }

        def data_ = [
                metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
                data    : [
                        repositories   : installedRepos.collect { [id: it.id, type: it.type, doInstall: it.doInstall, data: [git: [url: it.data.git == null ? null : it.data.git.url]]] },
                        sections       : sections,
                        tests          : SortUtil.sortIssuesByKey(installationTestIssues.collect { testIssue ->
                            [
                                    key     : testIssue.key,
                                    summary : testIssue.name,
                                    techSpec: testIssue.techSpecs.join(", ") ?: "N/A"
                            ]
                        }),
                        testsOdsService: testsOfRepoTypeOdsService,
                        testsOdsCode   : testsOfRepoTypeOdsCode
                ],
                documentHistory: docHistory?.getDocGenFormat() ?: [],
        ]

        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    String createSSDS(Map data) {
        log.info("createSSDS for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.SSDS as String

        def bbInfo = this.bbt.getPRMergeInfo(projectData)
        def sections = this.getDocumentSections(documentType, projectData)
        def watermarkText = this.getWatermarkText(projectData)

        def componentsMetadata = SortUtil.sortIssuesByKey(this.computeComponentMetadata(projectData, documentType).values())
        def systemDesignSpecifications = projectData.getTechnicalSpecifications()
                .findAll { it.systemDesignSpec }
                .collect { techSpec ->
                    [
                            key        : techSpec.key,
                            req_key    : techSpec.requirements?.join(", ") ?: "None",
                            description: this.convertImages(techSpec.systemDesignSpec)
                    ]
                }

        if (!sections."sec2s3") sections."sec2s3" = [:]
        sections."sec2s3".bitbucket = SortUtil.sortIssuesByProperties(bbInfo ?: [], ["component", "date", "url"])

        if (!sections."sec3s1") sections."sec3s1" = [:]
        sections."sec3s1".specifications = SortUtil.sortIssuesByProperties(systemDesignSpecifications, ["req_key", "key"])

        if (!sections."sec5s1") sections."sec5s1" = [:]
        sections."sec5s1".components = componentsMetadata.collect { c ->
            [
                    key           : c.key,
                    nameOfSoftware: c.nameOfSoftware,
                    componentType : c.componentType,
                    componentId   : c.componentId,
                    description   : this.convertImages(c.description ?: ''),
                    supplier      : c.supplier,
                    version       : c.version,
                    references    : c.references,
                    doInstall     : c.doInstall
            ]
        }

        // Get the components that we consider modules in SSDS (the ones you have to code)
        def modules = componentsMetadata
                .findAll {  it.odsRepoType.toLowerCase() == PipelineConfig.REPO_TYPE_ODS_CODE.toLowerCase() }
                .collect {  component ->
                    // We will set-up a double loop in the template. For moustache limitations we need to have lists
                    component.requirements = component.requirements.findAll { it != null }.collect { r ->
                        [key: r.key, name: r.name,
                         reqDescription: this.convertImages(r.description), gampTopic: r.gampTopic ?: "uncategorized"]
                    }.groupBy { it.gampTopic.toLowerCase() }
                            .collect { k, v -> [gampTopic: k, requirementsofTopic: v] }

                    return component
                }

        if (!sections."sec10") sections."sec10" = [:]
        sections."sec10".modules = modules

        def keysInDoc = this.computeKeysInDocForSSDS(projectData.getTechnicalSpecifications(), componentsMetadata, modules)
        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)
        def data_ = [
                metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
                data    : [
                        sections: sections,
                        documentHistory: docHistory?.getDocGenFormat() ?: [],
                ]
        ]
        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    String createTCP(Map data) {
        log.info("createTCP for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        String documentType = Constants.DocumentType.TCP as String

        def sections = this.getDocumentSections(documentType, projectData)
        def watermarkText = this.getWatermarkText(projectData)

        def integrationTestIssues = projectData.getAutomatedTestsTypeIntegration()
        def acceptanceTestIssues = projectData.getAutomatedTestsTypeAcceptance()

        def keysInDoc = computeKeysInDocForTCP(integrationTestIssues + acceptanceTestIssues)

        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)
        def data_ = [
                metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
                data    : [
                        sections        : sections,
                        integrationTests: SortUtil.sortIssuesByKey(integrationTestIssues.collect { testIssue ->
                            [
                                    key         : testIssue.key,
                                    description : this.convertImages(testIssue.description ?: ''),
                                    requirements: testIssue.requirements ? testIssue.requirements.join(", ") : "N/A",
                                    bugs        : testIssue.bugs ? testIssue.bugs.join(", ") : "N/A",
                                    steps       : sortTestSteps(testIssue.steps)
                            ]
                        }),
                        acceptanceTests : SortUtil.sortIssuesByKey(acceptanceTestIssues.collect { testIssue ->
                            [
                                    key         : testIssue.key,
                                    description : this.convertImages(testIssue.description ?: ''),
                                    requirements: testIssue.requirements ? testIssue.requirements.join(", ") : "N/A",
                                    bugs        : testIssue.bugs ? testIssue.bugs.join(", ") : "N/A",
                                    steps       : sortTestSteps(testIssue.steps)
                            ]
                        }),
                        documentHistory: docHistory?.getDocGenFormat() ?: [],
                ]
        ]

        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    String createTIP(Map data) {
        log.info("createTIP for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.TIP as String

        def sections = this.getDocumentSectionsFileOptional(projectData, documentType)
        def watermarkText = this.getWatermarkText(projectData)

        def keysInDoc = this.computeKeysInDocForTIP(projectData.getComponents())
        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)

        //def repositories = projectData.repositories.findAll {
        //    !Constants.COMPONENT_TYPE_IS_NOT_INSTALLED.contains(it.type?.toLowerCase())
        //}

        def data_ = [
                metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
                data    : [
                        project_key : projectData.key,
                        repositories: projectData.repositories.collect {
                            it << [ doInstall: !Constants.COMPONENT_TYPE_IS_NOT_INSTALLED.contains(it.type?.toLowerCase())]
                        },
                        sections    : sections,
                        documentHistory: docHistory?.getDocGenFormat() ?: [],
                ]
        ]

        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    String createTRC(Map data) {
        log.info("createTRC for ${data.projectBuild}")
        log.trace("createTRC - data:${data}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.TRC as String
        def sections = this.getDocumentSections(documentType, projectData)
        def systemRequirements = projectData.getSystemRequirements()

        def testIssues = systemRequirements
                .collect { it.getResolvedTests() }
                .flatten().unique().findAll{it != null}
                .findAll {
                    [TestType.ACCEPTANCE,
                     TestType.INSTALLATION,
                     TestType.INTEGRATION].contains(it.testType)
                }

        systemRequirements = systemRequirements.collect { r ->
            def predecessors = r.expandedPredecessors.collect { [key: it.key, versions: it.versions.join(', ')] }
            def testWithoutUnit = r.tests.collect()
            // Only if test key from requirements are also in testIssues (Acceptance, Integration, Installation) but no
            // Unit tests
            testWithoutUnit.retainAll(testIssues.key)
            [
                    key         : r.key,
                    name        : r.name,
                    description : this.convertImages(r.description ?: ''),
                    techSpecs   : r.techSpecs.join(", "),
                    risks       : (r.getResolvedTechnicalSpecifications().risks + r.risks).flatten().unique().join(", "),
                    tests       : testWithoutUnit.join(", "),
                    predecessors: predecessors,
            ]
        }

        if (!sections."sec4") sections."sec4" = [:]
        sections."sec4".systemRequirements = SortUtil.sortIssuesByKey(systemRequirements)

        def keysInDoc = this.computeKeysInDocForTRC(projectData.getSystemRequirements())
        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)

        def data_ = [
                metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
                data    : [
                        sections: sections,
                        documentHistory: docHistory?.getDocGenFormat() ?: [],
                ]
        ]

        def watermarkText = this.getWatermarkText(projectData)
        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    // DocTypeProjectFixtureWithTestData
    @SuppressWarnings('CyclomaticComplexity')
    String createTCR(Map data) {
        log.info("createTCR for ${data.projectBuild}")
        log.trace("createTCR - data:${data}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        String documentType = Constants.DocumentType.TCR as String

        def sections = this.getDocumentSections(documentType, projectData)
        def watermarkText = this.getWatermarkText(projectData)

        def integrationTestData = data.tests.integration
        def integrationTestIssues = projectData.getAutomatedTestsTypeIntegration()

        def acceptanceTestData = data.tests.acceptance
        def acceptanceTestIssues = projectData.getAutomatedTestsTypeAcceptance()

        def matchedHandler = { result ->
            result.each { testIssue, testCase ->
                testIssue.isSuccess = !(testCase.error || testCase.failure || testCase.skipped
                        || !testIssue.getResolvedBugs(). findAll { bug -> bug.status?.toLowerCase() != "done" }.isEmpty()
                        || testIssue.isUnexecuted)
                testIssue.comment = testIssue.isUnexecuted ? "This Test Case has not been executed" : ""
                testIssue.timestamp = testIssue.isUnexecuted ? "N/A" : testCase.timestamp
                testIssue.isUnexecuted = false
                testIssue.actualResult = testIssue.isSuccess ? "Expected result verified by automated test" :
                        testIssue.isUnexecuted ? "Not executed" : "Test failed. Correction will be tracked by Jira issue task \"bug\" listed below."
            }
        }

        def unmatchedHandler = { result ->
            result.each { testIssue ->
                testIssue.isSuccess = false
                testIssue.isUnexecuted = true
                testIssue.comment = testIssue.isUnexecuted ? "This Test Case has not been executed" : ""
                testIssue.actualResult = testIssue.isUnexecuted ? "Not executed" : "Test failed. Correction will be tracked by Jira issue task \"bug\" listed below."
            }
        }

        this.jiraUseCase.matchTestIssuesAgainstTestResults(integrationTestIssues, integrationTestData?.testResults ?: [:], matchedHandler, unmatchedHandler)
        this.jiraUseCase.matchTestIssuesAgainstTestResults(acceptanceTestIssues, acceptanceTestData?.testResults ?: [:], matchedHandler, unmatchedHandler)

        def keysInDoc = this.computeKeysInDocForTCR(integrationTestIssues + acceptanceTestIssues)
        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)

        def data_ = [
                metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
                data    : [
                        sections            : sections,
                        integrationTests    : SortUtil.sortIssuesByKey(integrationTestIssues.collect { testIssue ->
                            [
                                    key         : testIssue.key,
                                    description : this.convertImages(testIssue.description ?: ''),
                                    requirements: testIssue.requirements ? testIssue.requirements.join(", ") : "N/A",
                                    isSuccess   : testIssue.isSuccess,
                                    bugs        : testIssue.bugs ? testIssue.bugs.join(", ") : (testIssue.comment ? "": "N/A"),
                                    steps       : sortTestSteps(testIssue.steps),
                                    timestamp   : testIssue.timestamp ? testIssue.timestamp.replaceAll("T", " ") : "N/A",
                                    comment     : testIssue.comment,
                                    actualResult: testIssue.actualResult
                            ]
                        }),
                        acceptanceTests     : SortUtil.sortIssuesByKey(acceptanceTestIssues.collect { testIssue ->
                            [
                                    key         : testIssue.key,
                                    description : this.convertImages(testIssue.description ?: ''),
                                    requirements: testIssue.requirements ? testIssue.requirements.join(", ") : "N/A",
                                    isSuccess   : testIssue.isSuccess,
                                    bugs        : testIssue.bugs ? testIssue.bugs.join(", ") : (testIssue.comment ? "": "N/A"),
                                    steps       : sortTestSteps(testIssue.steps),
                                    timestamp   : testIssue.timestamp ? testIssue.timestamp.replaceAll("T", " ") : "N/A",
                                    comment     : testIssue.comment,
                                    actualResult: testIssue.actualResult
                            ]
                        }),
                        integrationTestFiles: SortUtil.sortIssuesByProperties(integrationTestData.testReportFiles.collect { file ->
                            [name: file.name, path: file.path, text: file.text]
                        } ?: [], ["name"]),
                        acceptanceTestFiles : SortUtil.sortIssuesByProperties(acceptanceTestData.testReportFiles.collect { file ->
                            [name: file.name, path: file.path, text: file.text]
                        } ?: [], ["name"]),
                        documentHistory: docHistory?.getDocGenFormat() ?: [],
                ]
        ]

        def uri = this.createDocument(projectData, documentType, null, data_, [:], null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    @SuppressWarnings('CyclomaticComplexity')
    String createCFTR(Map data) {
        log.info("createCFTR for ${data.projectBuild}")
        log.trace("createCFTR - data:${data}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.CFTR as String
        def acceptanceTestData = data.tests.acceptance
        def integrationTestData = data.tests.integration

        def sections = this.getDocumentSections(documentType, projectData)
        def watermarkText = this.getWatermarkText(projectData)

        def acceptanceTestIssues = SortUtil.sortIssuesByKey(projectData.getAutomatedTestsTypeAcceptance())
        def integrationTestIssues = SortUtil.sortIssuesByKey(projectData.getAutomatedTestsTypeIntegration())
        def discrepancies = this
                .computeTestDiscrepancies("Integration and Acceptance Tests",
                        (acceptanceTestIssues + integrationTestIssues),
                        junit.combineTestResults([acceptanceTestData.testResults, integrationTestData.testResults]),
                        false)

        def keysInDoc = this.computeKeysInDocForCFTR(integrationTestIssues + acceptanceTestIssues)

        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)

        def data_ = [
                metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
                data    : [
                        sections                     : sections,
                        numAdditionalAcceptanceTests : junit.getNumberOfTestCases(acceptanceTestData.testResults) - acceptanceTestIssues.count { !it.isUnexecuted },
                        numAdditionalIntegrationTests: junit.getNumberOfTestCases(integrationTestData.testResults) - integrationTestIssues.count { !it.isUnexecuted },
                        conclusion                   : [
                                summary  : discrepancies.conclusion.summary,
                                statement: discrepancies.conclusion.statement
                        ],
                        documentHistory: docHistory?.getDocGenFormat() ?: [],
                ]
        ]

        if (!acceptanceTestIssues.isEmpty()) {
            data_.data.acceptanceTests = acceptanceTestIssues.collect { testIssue ->
                [
                        key        : testIssue.key,
                        datetime   : testIssue.timestamp ? testIssue.timestamp.replaceAll("T", "</br>") : "N/A",
                        description: getTestDescription(testIssue),
                        remarks    : testIssue.isUnexecuted ? "Not executed" : "",
                        risk_key   : testIssue.risks ? testIssue.risks.join(", ") : "N/A",
                        success    : testIssue.isSuccess ? "Y" : "N",
                        ur_key     : testIssue.requirements ? testIssue.requirements.join(", ") : "N/A"
                ]
            }
        }

        if (!integrationTestIssues.isEmpty()) {
            data_.data.integrationTests = integrationTestIssues.collect { testIssue ->
                [
                        key        : testIssue.key,
                        datetime   : testIssue.timestamp ? testIssue.timestamp.replaceAll("T", "</br>") : "N/A",
                        description: getTestDescription(testIssue),
                        remarks    : testIssue.isUnexecuted ? "Not executed" : "",
                        risk_key   : testIssue.risks ? testIssue.risks.join(", ") : "N/A",
                        success    : testIssue.isSuccess ? "Y" : "N",
                        ur_key     : testIssue.requirements ? testIssue.requirements.join(", ") : "N/A"
                ]
            }
        }

        def files = (acceptanceTestData.testReportFiles + integrationTestData.testReportFiles).collectEntries { file ->
            ["raw/${file.getName()}", file.getBytes()]
        }

        def uri = this.createDocument(projectData, documentType, null, data_, files, null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    String createIVR(Map data) {
        log.info("createIVR for ${data.projectBuild}")
        log.trace("createIVR - data:${data}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.IVR as String

        def installationTestData = data.tests.installation

        def sections = this.getDocumentSections(documentType, projectData)
        def watermarkText = this.getWatermarkText(projectData)

        def installationTestIssues = projectData.getAutomatedTestsTypeInstallation()
        def discrepancies = this.computeTestDiscrepancies("Installation Tests", installationTestIssues, installationTestData.testResults)

        def testsOfRepoTypeOdsCode = []
        def testsOfRepoTypeOdsService = []
        def testsGroupedByRepoType = groupTestsByRepoType(installationTestIssues)
        testsGroupedByRepoType.each { repoTypes, tests ->
            if (repoTypes.contains(PipelineConfig.REPO_TYPE_ODS_CODE)) {
                testsOfRepoTypeOdsCode.addAll(tests)
            }

            if (repoTypes.contains(PipelineConfig.REPO_TYPE_ODS_SERVICE)) {
                testsOfRepoTypeOdsService.addAll(tests)
            }
        }

        def keysInDoc =  this.computeKeysInDocForIVR(installationTestIssues)
        def docHistory = this.getAndStoreDocumentHistory(documentType, keysInDoc, projectData)

        def installedRepos = projectData.repositories.collect {
            it << [ doInstall: !Constants.COMPONENT_TYPE_IS_NOT_INSTALLED.contains(it.type?.toLowerCase())]
        }

        def data_ = [
                metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType]),
                data    : [
                        repositories   : installedRepos.collect { [id: it.id, type: it.type, doInstall: it.doInstall, data: [git: [url: it.data.git == null ? null : it.data.git.url]]] },
                        sections          : sections,
                        tests             : SortUtil.sortIssuesByKey(installationTestIssues.collect { testIssue ->
                            [
                                    key        : testIssue.key,
                                    description: this.convertImages(testIssue.description ?: ''),
                                    remarks    : testIssue.isUnexecuted ? "Not executed" : "",
                                    success    : testIssue.isSuccess ? "Y" : "N",
                                    summary    : testIssue.name,
                                    techSpec   : testIssue.techSpecs.join(", ") ?: "N/A"
                            ]
                        }),
                        numAdditionalTests: junit.getNumberOfTestCases(installationTestData.testResults) - installationTestIssues.count { !it.isUnexecuted },
                        testFiles         : SortUtil.sortIssuesByProperties(installationTestData.testReportFiles.collect { file ->
                            [name: file.name, path: file.path, text: file.text]
                        } ?: [], ["name"]),
                        discrepancies     : discrepancies.discrepancies,
                        conclusion        : [
                                summary  : discrepancies.conclusion.summary,
                                statement: discrepancies.conclusion.statement
                        ],
                        testsOdsService   : testsOfRepoTypeOdsService,
                        testsOdsCode      : testsOfRepoTypeOdsCode
                ],
                documentHistory: docHistory?.getDocGenFormat() ?: [],
        ]

        def files = data.tests.installation.testReportFiles.collectEntries { file ->
            ["raw/${file.getName()}", file.getBytes()]
        }

        def uri = this.createDocument(projectData, documentType, null, data_, files, null, getDocumentTemplateName(projectData, documentType), watermarkText)
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docHistory?.getVersion() as String)
        return uri
    }

    // DocTypeProjectFixtureWithComponent
    String createDTR(Map data) {
        log.info("createDTR for ${data.projectBuild}")
        log.trace("createDTR - data:${prettyPrint(toJson(data))}")

        Map repo = data.repo
        Map repoData = data.repo.data
        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)

        def documentType = Constants.DocumentType.DTR as String
        Map resurrectedDocument = resurrectAndStashDocument(projectData, documentType, repo)
        log.info "Resurrected ${documentType} for ${repo.id} -> (${resurrectedDocument.found})"
        if (resurrectedDocument.found) {
            return resurrectedDocument.uri
        }

        def unitTestData = repoData.tests.unit

        def sections = this.getDocumentSectionsFileOptional(projectData, documentType)
        def watermarkText = this.getWatermarkText(projectData)

        def testIssues = projectData.getAutomatedTestsTypeUnit("Technology-${repo.id}")
        def discrepancies = this.computeTestDiscrepancies("Development Tests", testIssues, unitTestData.testResults)

        def obtainEnum = { category, value ->
            return projectData.getEnumDictionary(category)[value as String]
        }

        def tests = testIssues.collect { testIssue ->
            def description = ''
            if (testIssue.description) {
                description += testIssue.description
            } else {
                description += testIssue.name
            }

            def riskLevels = testIssue.getResolvedRisks(). collect {
                def value = obtainEnum("SeverityOfImpact", it.severityOfImpact)
                return value ? value.text : "None"
            }

            def softwareDesignSpecs = testIssue.getResolvedTechnicalSpecifications()
                    .findAll { it.softwareDesignSpec }
                    .collect { it.key }

            [
                    key               : testIssue.key,
                    description       : this.convertImages(description ?: 'N/A'),
                    systemRequirement : testIssue.requirements.join(", "),
                    success           : testIssue.isSuccess ? "Y" : "N",
                    remarks           : testIssue.isUnexecuted ? "Not executed" : "N/A",
                    softwareDesignSpec: (softwareDesignSpecs.join(", ")) ?: "N/A",
                    riskLevel         : riskLevels ? riskLevels.join(", ") : "N/A"
            ]
        }

        def keysInDoc = this.computeKeysInDocForDTR(tests)
        DocumentHistory docHistory = this.getAndStoreDocumentHistory(documentType + '-' + repo.id, keysInDoc, projectData)

        def data_ = [
                metadata: this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType], repo),
                data    : [
                        repo              : repo,
                        sections          : sections,
                        tests             : tests,
                        numAdditionalTests: junit.getNumberOfTestCases(unitTestData.testResults) - testIssues.count { !it.isUnexecuted },
                        testFiles         : SortUtil.sortIssuesByProperties(unitTestData.testReportFiles.collect { file ->
                            [name: file.name, path: file.path, text: XmlUtil.serialize(file.text)]
                        } ?: [], ["name"]),
                        discrepancies     : discrepancies.discrepancies,
                        conclusion        : [
                                summary  : discrepancies.conclusion.summary,
                                statement: discrepancies.conclusion.statement
                        ],
                        documentHistory: docHistory?.getDocGenFormat() ?: [],
                ]
        ]

        def files = unitTestData.testReportFiles.collectEntries { file ->
            ["raw/${file.getName()}", file.getBytes()]
        }

        def modifier = { document -> return document }
        def templateName = getDocumentTemplateName(projectData, documentType, repo)
        return this.createDocument(projectData, documentType, repo, data_, files, modifier, templateName, watermarkText)
    }

    @SuppressWarnings('CyclomaticComplexity')
    String createTIR(Map data) {
        log.info("createTIR for ${data.projectBuild}")
        log.trace("createTIR - data:${prettyPrint(toJson(data))}")

        Map repo = data.repo
        Map repoData = data.repo.data
        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentType = Constants.DocumentType.TIR as String

        def installationTestData = repoData?.tests?.installation

        def sections = this.getDocumentSectionsFileOptional(projectData, documentType)
        def watermarkText = this.getWatermarkText(projectData)

        def deploynoteData = 'Components were built & deployed during installation.'
        if (repo.data.openshift.resurrectedBuild) {
            deploynoteData = "Components were found, and are 'up to date' with version control -no deployments happend!\r" +
                    " SCRR was restored from the corresponding creation build (${repo.data.openshift.resurrectedBuild})"
        } else if (!repo.data.openshift.builds) {
            deploynoteData = 'NO Components were built during installation, existing components (created in Dev) were deployed.'
        }

        def keysInDoc = ['Technology-' + repo.id]
        def docHistory = this.getAndStoreDocumentHistory(documentType + '-' + repo.id, keysInDoc,projectData)

        repo << [ doInstall: !Constants.COMPONENT_TYPE_IS_NOT_INSTALLED.contains(repo.type?.toLowerCase())]

        def data_ = [
                metadata     : this.getDocumentMetadata(projectData, Constants.DOCUMENT_TYPE_NAMES[documentType], repo),
                deployNote   : deploynoteData,
                openShiftData: [
                        builds     : repo.data.openshift.builds ?: '',
                        deployments: repo.data.openshift.deployments ?: ''
                ],
                testResults: [
                        installation: installationTestData?.testResults
                ],
                data: [
                        repo    : repo,
                        sections: sections,
                        documentHistory: docHistory?.getDocGenFormat() ?: [],
                ]
        ]

        def modifier = { document -> return document }
        return this.createDocument(projectData, documentType, repo, data_, [:], modifier, getDocumentTemplateName(projectData, documentType, repo), watermarkText)
    }

    String createOverallDTR(Map data) {
        log.info("createOverallDTR for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentTypeName = Constants.DOCUMENT_TYPE_NAMES[Constants.DocumentType.OVERALL_DTR as String]
        def metadata = this.getDocumentMetadata(projectData, documentTypeName)
        def documentType = Constants.DocumentType.DTR as String

        def watermarkText = this.getWatermarkText(projectData)

        def uri = this.createOverallDocument('Overall-Cover', documentType, metadata, null, watermarkText, projectData)
        def docVersion = projectData.getDocumentVersionFromHistories(documentType) as String
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docVersion)
        return uri
    }

    String createOverallTIR(Map data) {
        log.info("createOverallTIR for ${data.projectBuild}")

        ProjectData projectData = project.getProjectData(data.projectBuild as String, data)
        def documentTypeName = Constants.DOCUMENT_TYPE_NAMES[Constants.DocumentType.OVERALL_TIR as String]
        def metadata = this.getDocumentMetadata(projectData, documentTypeName)

        def documentType = Constants.DocumentType.TIR as String

        def watermarkText = this.getWatermarkText(projectData)

        def visitor = { data_ ->
            // Prepend a section for the Jenkins build log
            data_.sections.add(0, [
                    heading: 'Installed Component Summary'
            ])
            data_.sections.add(1, [
                    heading: 'Jenkins Build Log'
            ])

            // Add Jenkins build log data
            data_.jenkinsData = [
                    log: this.jenkins.getCurrentBuildLogAsText()
            ]

            data_.repositories = projectData.repositories.collect {
                it << [ doInstall: !Constants.COMPONENT_TYPE_IS_NOT_INSTALLED.contains(it.type?.toLowerCase())]
            }
        }

        def uri = this.createOverallDocument('Overall-TIR-Cover', documentType, metadata, visitor, watermarkText, projectData)
        def docVersion = projectData.getDocumentVersionFromHistories(documentType) as String
        this.updateJiraDocumentationTrackingIssue(projectData,  documentType, uri, docVersion)
        return uri
    }

    private def computeKeysInDocForDTR(def data) {
        return data.collect {
            [it.key, it.systemRequirement.split(', '), it.softwareDesignSpec.split(', ')]
        }.flatten()
    }

    private def computeKeysInDocForCFTR(def data) {
        return data.collect { it.subMap(['key']).values() }.flatten()
    }

    //TODO Use this method to generate the test description everywhere
    def getTestDescription(testIssue) {
        return testIssue.description ?: testIssue.name ?: 'N/A'
    }
    
    private def computeKeysInDocForRA(def data) {
        return data
                .collect { it.subMap(['key', 'requirements', 'techSpecs', 'mitigations', 'tests']).values()  }
                .flatten()
    }
    
    private def computeKeysInDocForIPV(def data) {
        return data
                .collect { it.subMap(['key', 'components', 'techSpecs']).values()  }
                .flatten()
    }
    
    private def computeKeysInDocForIVR(def data) {
        return data
                .collect { it.subMap(['key', 'components', 'techSpecs']).values()  }
                .flatten()
    }
    
    def sortTestSteps(def testSteps) {
        return testSteps?.sort(false) { it.orderId }
    }
    
    private def computeKeysInDocForSSDS(def techSpecs, def componentsMetadata, def modules) {
        def specs = techSpecs.collect { it.subMap(['key', 'requirements']).values() }.flatten()
        def components = componentsMetadata.collect { it.key }
        def mods = modules.collect { it.subMap(['requirementKeys', 'softwareDesignSpecKeys']).values() }.flatten()
        return specs + components + mods
    }

    private def computeKeysInDocForTIP(def data) {
        return data.collect { it.key }
    }

    private def computeKeysInDocForTRC(def data) {
        return data.collect { it.subMap(['key', 'risks', 'tests']).values()  }.flatten()
    }

    private String getDocumentTemplateName(ProjectData projectData, String documentType, Map repo = null) {
        def capability = projectData.getCapability("LeVADocs")
        if (!capability) {
            return documentType
        }

        def suffix = ""
        // compute suffix based on repository type
        if (repo != null) {
            if (repo.type.toLowerCase() == PipelineConfig.REPO_TYPE_ODS_INFRA) {
                if (documentType == Constants.DocumentType.TIR as String) {
                    suffix += "-infra"
                }
            }
        }

        // compute suffix based on gamp category
        if (Constants.GAMP_CATEGORY_SENSITIVE_DOCS.contains(documentType)) {
            suffix += "-" + capability.GAMPCategory
        }

        return documentType + suffix
    }
    
    private computeKeysInDocForTCP(def data) {
        return data.collect { it.subMap(['key', 'requirements', 'bugs']).values() }.flatten()
    }

    
    List<String> getSupportedDocuments() {
        return Constants.DocumentType.values().collect { it as String }
    }

    String getDocumentTemplatesVersion(ProjectData projectData) {
        def capability = projectData.getCapability('LeVADocs')
        return capability.templatesVersion
    }

    boolean shouldCreateArtifact (String documentType, Map repo) {
        List nonArtifactDocTypes = [
            Constants.DocumentType.TIR as String,
            Constants.DocumentType.DTR as String
        ]

        return !(documentType && nonArtifactDocTypes.contains(documentType) && repo)
    }

    Map getFiletypeForDocumentType (String documentType) {
        if (!documentType) {
            throw new RuntimeException ('Cannot lookup Null docType for storage!')
        }
        Map defaultTypes = [storage: 'zip', content: 'pdf' ]

        if (Constants.DOCUMENT_TYPE_NAMES.containsKey(documentType)) {
            return defaultTypes
        } else if (Constants.DOCUMENT_TYPE_FILESTORAGE_EXCEPTIONS.containsKey(documentType)) {
            return Constants.DOCUMENT_TYPE_FILESTORAGE_EXCEPTIONS.get(documentType)
        }
        return defaultTypes
    }

    protected String convertImages(String content) {
        def result = content
        if (content && content.contains("<img")) {
            result = this.jiraUseCase.convertHTMLImageSrcIntoBase64Data(content)
        }
        result
    }

    protected Map computeTestDiscrepancies(String name, List testIssues, Map testResults, boolean checkDuplicateTestResults = true) {
        def result = [
            discrepancies: 'No discrepancies found.',
            conclusion   : [
                summary  : 'Complete success, no discrepancies',
                statement: "It is determined that all steps of the ${name} have been successfully executed and signature of this report verifies that the tests have been performed according to the plan. No discrepancies occurred.",
            ]
        ]

        // Match Jira test issues with test results
        def matchedHandler = { matched ->
            matched.each { testIssue, testCase ->
                testIssue.isSuccess = !(testCase.error || testCase.failure || testCase.skipped)
                testIssue.isUnexecuted = !!testCase.skipped
                testIssue.timestamp = testCase.timestamp
            }
        }

        def unmatchedHandler = { unmatched ->
            unmatched.each { testIssue ->
                testIssue.isSuccess = false
                testIssue.isUnexecuted = true
            }
        }

        this.jiraUseCase.matchTestIssuesAgainstTestResults(testIssues, testResults ?: [:], matchedHandler, unmatchedHandler, checkDuplicateTestResults)

        // Compute failed and missing Jira test issues
        def failedTestIssues = testIssues.findAll { testIssue ->
            return !testIssue.isSuccess && !testIssue.isUnexecuted
        }

        def unexecutedTestIssues = testIssues.findAll { testIssue ->
            return !testIssue.isSuccess && testIssue.isUnexecuted
        }

        // Compute extraneous failed test cases
        def extraneousFailedTestCases = []
        testResults.testsuites.each { testSuite ->
            extraneousFailedTestCases.addAll(testSuite.testcases.findAll { testCase ->
                return (testCase.error || testCase.failure) && !failedTestIssues.any { this.jiraUseCase.checkTestsIssueMatchesTestCase(it, testCase) }
            })
        }

        // Compute test discrepancies
        def isMajorDiscrepancy = failedTestIssues || unexecutedTestIssues || extraneousFailedTestCases
        if (isMajorDiscrepancy) {
            result.discrepancies = 'The following major discrepancies were found during testing.'
            result.conclusion.summary = 'No success - major discrepancies found'
            result.conclusion.statement = 'Some discrepancies found as'

            if (failedTestIssues || extraneousFailedTestCases) {
                result.conclusion.statement += ' tests did fail'
            }

            if (failedTestIssues) {
                result.discrepancies += " Failed tests: ${failedTestIssues.collect { it.key }.join(', ')}."
            }

            if (extraneousFailedTestCases) {
                result.discrepancies += " Other failed tests: ${extraneousFailedTestCases.size()}."
            }

            if (unexecutedTestIssues) {
                result.discrepancies += " Unexecuted tests: ${unexecutedTestIssues.collect { it.key }.join(', ')}."

                if (failedTestIssues || extraneousFailedTestCases) {
                    result.conclusion.statement += ' and others were not executed'
                } else {
                    result.conclusion.statement += ' tests were not executed'
                }
            }

            result.conclusion.statement += '.'
        }

        return result
    }

    protected List<Map> computeTestsWithRequirementsAndSpecs(ProjectData projectData, List<Map> tests) {
        def obtainEnum = { category, value ->
            return projectData.getEnumDictionary(category)[value as String]
        }

        tests.collect { testIssue ->
            def softwareDesignSpecs = testIssue.getResolvedTechnicalSpecifications()
                .findAll { it.softwareDesignSpec }
                .collect { it.key }
            def riskLevels = testIssue.getResolvedRisks(). collect {
                def value = obtainEnum("SeverityOfImpact", it.severityOfImpact)
                return value ? value.text : "None"
            }
            def description = ''
            if (testIssue.description) {
                description += testIssue.description
            } else {
                description += testIssue.name
            }

            [
                moduleName: testIssue.components.join(", "),
                testKey: testIssue.key,
                description: this.convertImages(description ?: 'N/A'),
                systemRequirement: testIssue.requirements ? testIssue.requirements.join(", ") : "N/A",
                softwareDesignSpec: (softwareDesignSpecs.join(", ")) ?: "N/A",
                riskLevel: riskLevels ? riskLevels.join(", ") : "N/A"
            ]
        }
    }

    protected List obtainCodeReviewReport(ProjectData projectData, List<Map> repos) {
        def reports =  repos.collect { r ->
            // resurrect?
            Map resurrectedDocument = resurrectAndStashDocument(projectData,'SCRR-MD', r, false)
            log.info "Resurrected 'SCRR' for ${r.id} -> (${resurrectedDocument.found})"
            if (resurrectedDocument.found) {
                return resurrectedDocument.content
            }

            def sqReportsPath = "${SONARQUBE_BASE_DIR}/${r.id}"
            def sqReportsStashName = "scrr-report-${r.id}-${projectData.build.buildId}"

            // Unstash SonarQube reports into path
            def hasStashedSonarQubeReports = this.jenkins.unstashFilesIntoPath(sqReportsStashName, "${projectData.tmpFolder}/${sqReportsPath}", "SonarQube Report")
            if (!hasStashedSonarQubeReports) {
                throw new RuntimeException("Error: unable to unstash SonarQube reports for repo '${r.id}' from stash '${sqReportsStashName}'.")
            }

            // Load SonarQube report files from path
            def sqReportFiles = this.sq.loadReportsFromPath("${projectData.tmpFolder}/${sqReportsPath}")
            if (sqReportFiles.isEmpty()) {
                throw new RuntimeException("Error: unable to load SonarQube reports for repo '${r.id}' from path '${projectData.tmpFolder}/${sqReportsPath}'.")
            }

            def name = this.getDocumentBasename('SCRR-MD', projectData.build.version, projectData.build.buildId, r)
            def sqReportFile = sqReportFiles.first()

            def generatedSCRR = this.pdf.convertFromMarkdown(sqReportFile, true)

            // store doc - we may need it later for partial deployments
            if (!resurrectedDocument.found) {
                def result = this.storeDocument("${name}.pdf", generatedSCRR, 'application/pdf')
                log.info "Stored 'SCRR' for later consumption -> ${result}"
            }
            return generatedSCRR
        }

        return reports
    }

    /**
     * This computes the information related to the components (modules) that are being developed
     * @documentType documentType
     * @return component metadata with software design specs, requirements and info comming from the component repo
     */
    protected Map computeComponentMetadata(ProjectData projectData, String documentType) {
        return projectData.components.collectEntries { component ->
            def normComponentName = component.name.replaceAll('Technology-', '')

            if (isReleaseManagerComponent(projectData, normComponentName)) {
                return [ : ]
            }

            def repo_ = projectData.repositories.find {
                [it.id, it.name, it.metadata.name].contains(normComponentName)
            }
            if (!repo_) {
                List<Map> repoNamesAndIds = projectData.repositories. collect { [id: it.id, name: it.name] }
                throw new RuntimeException("Error: unable to create ${documentType}. Could not find a repository " +
                    "configuration with id or name equal to '${normComponentName}' for " +
                    "Jira component '${component.name}' in project '${projectData.key}'. Please check " +
                    "the metatada.yml file. In this file there are the following repositories " +
                    "configured: ${repoNamesAndIds}")
            }

            def metadata = repo_.metadata

            def sowftwareDesignSpecs = component.getResolvedTechnicalSpecifications()
                .findAll { it.softwareDesignSpec }
                .collect { [key: it.key, softwareDesignSpec: this.convertImages(it.softwareDesignSpec)] }

            return [
                (component.name): [
                    key               : component.name,
                    componentName     : component.name,
                    componentId       : metadata.id ?: 'N/A - part of this application',
                    componentType     : Constants.INTERNAL_TO_EXT_COMPONENT_TYPES.get(repo_.type?.toLowerCase()),
                    doInstall         : !Constants.COMPONENT_TYPE_IS_NOT_INSTALLED.contains(repo_.type?.toLowerCase()),
                    odsRepoType       : repo_.type?.toLowerCase(),
                    description       : metadata.description,
                    nameOfSoftware    : normComponentName ?: metadata.name,
                    references        : metadata.references ?: 'N/A',
                    supplier          : metadata.supplier,
                    version           : (repo_.type?.toLowerCase() == PipelineConfig.REPO_TYPE_ODS_CODE) ?
                        projectData.build.version :
                        metadata.version,
                    requirements      : component.getResolvedSystemRequirements(),
                    requirementKeys   : component.requirements,
                    softwareDesignSpecKeys: sowftwareDesignSpecs.collect { it.key },
                    softwareDesignSpec: sowftwareDesignSpecs
                ]
            ]
        }
    }

    private boolean isReleaseManagerComponent(ProjectData projectData, normComponentName) {
        def gitUrl = projectData.data.git.url // TODO s2o review this code
        return gitUrl?.endsWith("${projectData.key}-${normComponentName}".toLowerCase())
    }

    protected Map computeComponentsUnitTests(List<Map> tests) {
        def issueComponentMapping = tests.collect { test ->
            test.getResolvedComponents().collect { [test: test.key, component: it.name] }
        }.flatten()
        issueComponentMapping.groupBy { it.component }.collectEntries { c, v ->
            [(c.replaceAll("Technology-", "")): v.collect { it.test } ]
        }
    }

    protected List<Map> getReposWithUnitTestsInfo(ProjectData projectData, List<Map> unitTests) {
        def componentTestMapping = computeComponentsUnitTests(unitTests)
        projectData.repositories.collect {
            [
                id: it.id,
                description: it.metadata?.description,
                tests: componentTestMapping[it.id]? componentTestMapping[it.id].join(", "): "None defined"
            ]
        }
    }

    private Map groupTestsByRepoType(List jiraTestIssues) {
        return jiraTestIssues.collect { test ->
            def components = test.getResolvedComponents()
            test.repoTypes = components.collect { component ->
                def normalizedComponentName = component.name.replaceAll('Technology-', '')
                def repository = projectData.repositories.find { repository ->
                    [repository.id, repository.name].contains(normalizedComponentName)
                }

                if (!repository) {
                    throw new IllegalArgumentException("Error: unable to find a repository definition with id or name equal to '${normalizedComponentName}' for Jira component '${component.name}' in project '${projectData.id}'.")
                }

                return repository.type
            } as Set

            return test
        }.groupBy { it.repoTypes }
    }

    protected Map getDocumentMetadata(ProjectData projectData, String documentTypeName, Map repo = null) {
        def name = projectData.name
        if (repo) {
            name += ": ${repo.id}"
        }

        def metadata = [
            id            : null, // unused
            name          : name,
            description   : projectData.description,
            type          : documentTypeName,
            version       : projectData.build.version,
            date_created  : LocalDateTime.now(clock).toString(),
            buildParameter: projectData.build,
            git           : repo ? repo.data.git : projectData.gitData,
            openShift     : [apiUrl: projectData.getOpenShiftApiUrl()],
            jenkins       : [
                buildNumber: projectData.build.buildNumber,
                buildUrl   : projectData.build.buildURL,
                jobName    : projectData.build.jobName
            ],
            referencedDocs : this.getReferencedDocumentsVersion(projectData)
        ]

        metadata.header = ["${documentTypeName}, Config Item: ${metadata.buildParameter.configItem}", "Doc ID/Version: see auto-generated cover page"]

        return metadata
    }

    private List<String> getJiraTrackingIssueLabelsForDocTypeAndEnvs(ProjectData projectData, String documentType, List<String> envs = null) {
        def labels = []

        def environments = envs ?: projectData.build.targetEnvironmentToken
        environments.each { env ->
            Environment.ENVIRONMENT_TYPE[env].get(documentType).each { label ->
                labels.add("${LabelPrefix.DOCUMENT}${label}")
            }
        }

        if (projectData.isDeveloperPreviewMode()) {
            // Assumes that every document we generate along the pipeline has a tracking issue in Jira
            labels.add("${LabelPrefix.DOCUMENT}${documentType}")
        }

        return labels
    }

    protected String getWatermarkText(ProjectData projectData) {
        def result = null

        if (projectData.isDeveloperPreviewMode()) {
            result = Constants.DEVELOPER_PREVIEW_WATERMARK
        }

        if (projectData.hasWipJiraIssues()) {
            result = Constants.WORK_IN_PROGRESS_WATERMARK
        }

        return result
    }

    private void updateJiraDocumentationTrackingIssue(ProjectData projectData,
                                                      String documentType,
                                                      String docLocation,
                                                      String documentVersionId = null) {
        def jiraIssues = this.getDocumentTrackingIssues(projectData, documentType)
        def msg = "A new ${Constants.DOCUMENT_TYPE_NAMES[documentType]} has been generated and is available at: ${docLocation}."
        def sectionsNotDone = projectData.getWIPDocChaptersForDocument(documentType)
        // Append a warning message for documents which are considered work in progress
        if (!sectionsNotDone.isEmpty()) {
            msg += " ${Constants.WORK_IN_PROGRESS_DOCUMENT_MESSAGE} See issues:" +
                " ${sectionsNotDone.join(', ')}"
        }

        // Append a warning message if there are any open tasks. Documents will not be considered final
        // TODO review me
        if (documentVersionId && !projectData.isDeveloperPreviewMode() && projectData.hasWipJiraIssues()) {
            msg += "\n *Since there are WIP issues in Jira that affect one or more documents," +
                " this document cannot be considered final.*"
        }

        if (! documentVersionId) {
            def metadata = this.getDocumentMetadata(projectData, documentType)
            documentVersionId = "${metadata.version}-${metadata.jenkins.buildNumber}"
        }

        jiraIssues.each { Map jiraIssue ->
            this.updateValidDocVersionInJira(jiraIssue.key as String, documentVersionId, projectData)
            this.jiraUseCase.jira.appendCommentToIssue(jiraIssue.key as String, msg)
        }
    }

    private List<String> computeSectionsNotDone(Map issues = [:]) {
        if (!issues) return []
        return issues.values().findAll { !it.status?.equalsIgnoreCase('done') }.collect { it.key }
    }

    private DocumentHistory getAndStoreDocumentHistory(String documentName, 
                                                       List<String> keysInDoc = [], 
                                                       ProjectData projectData) {
        // If we have already saved the version, load it from project
        if (projectData.historyForDocumentExists(documentName)) {
            return projectData.getHistoryForDocument(documentName)
        } else {
            def documentType = LeVADocumentUtil.getTypeFromName(documentName)
            def jiraData = projectData.data.jira as Map
            def environment = this.computeSavedDocumentEnvironment(documentType, projectData)
            def docHistory = new DocumentHistory(environment, documentName)
            def docChapters = projectData.getDocumentChaptersForDocument(documentType)
            def docChapterKeys = docChapters.collect { chapter ->
                chapter.key
            }
            docHistory.load(projectData, jiraData, (keysInDoc + docChapterKeys).unique())

            // Save the doc history to project class, so it can be persisted when considered
            projectData.setHistoryForDocument(docHistory, documentName)

            return docHistory
        }
    }

    private String computeSavedDocumentEnvironment(String documentType, ProjectData projectData) {
        def environment = projectData.build.targetEnvironmentToken
        if (projectData.isWorkInProgress) {
            environment = Environment.getEnvironment(documentType)
        }
        return environment
    }

    private void updateValidDocVersionInJira(String jiraIssueKey, String docVersionId, ProjectData projectData) {
        def documentationTrackingIssueFields = projectData.getJiraFieldsForIssueType(IssueTypes.DOCUMENTATION_TRACKING)
        def documentationTrackingIssueDocumentVersionField = documentationTrackingIssueFields[CustomIssueFields.DOCUMENT_VERSION]

        if (projectData.isVersioningEnabled) {
            if (!projectData.isDeveloperPreviewMode() && !projectData.hasWipJiraIssues()) {
                // In case of generating a final document, we add the label for the version that should be released
                this.jiraUseCase.jira.updateTextFieldsOnIssue(jiraIssueKey,
                    [(documentationTrackingIssueDocumentVersionField.id): "${docVersionId}"])
            }
        } else {
            // TODO removeme for ODS 4.0
            this.jiraUseCase.jira.updateTextFieldsOnIssue(jiraIssueKey,
                [(documentationTrackingIssueDocumentVersionField.id): "${docVersionId}"])
        }
    }

    private List<Map> getDocumentTrackingIssues(ProjectData projectData, String documentType, List<String> environments = null) {
        def jiraDocumentLabels = this.getJiraTrackingIssueLabelsForDocTypeAndEnvs(projectData, documentType, environments)
        def jiraIssues = projectData.getDocumentTrackingIssues(jiraDocumentLabels)
        if (jiraIssues.isEmpty()) {
            throw new RuntimeException("Error: no Jira tracking issue associated with document type '${documentType}'.")
        }
        return jiraIssues
    }

    private List<Map> getDocumentTrackingIssuesForHistory(ProjectData projectData, String documentType, List<String> environments = null) {
        def jiraDocumentLabels = this.getJiraTrackingIssueLabelsForDocTypeAndEnvs(projectData, documentType, environments)
        def jiraIssues = projectData.getDocumentTrackingIssuesForHistory(jiraDocumentLabels)
        if (jiraIssues.isEmpty()) {
            throw new RuntimeException("Error: no Jira tracking issue associated with document type '${documentType}'.")
        }
        return jiraIssues
    }

    private Map getDocumentSections(String documentType, ProjectData projectData) {
        def sections = projectData.getDocumentChaptersForDocument(documentType)

        if (!sections) {
            throw new RuntimeException("Error: unable to create ${documentType}. " +
                'Could not obtain document chapter data from Jira.')
        }
        // Extract-out the section, as needed for the DocGen interface
        return sections.collectEntries { sec ->
            [(sec.section): sec + [content: this.convertImages(sec.content)]]
        }
    }

    private Map getDocumentSectionsFileOptional(ProjectData projectData, String documentType) {
        def sections = projectData.getDocumentChaptersForDocument(documentType)
        sections = sections?.collectEntries { sec ->
            [(sec.section): sec + [content: this.convertImages(sec.content)]]
        }
        if (!sections || sections.isEmpty() ) {
            sections = this.levaFiles.getDocumentChapterData(projectData, documentType)
            if (!projectData.data.jira.undoneDocChapters) {
                projectData.data.jira.undoneDocChapters = [:]
            }
            projectData.data.jira.undoneDocChapters[documentType] = this.computeSectionsNotDone(sections)
            sections = sections?.collectEntries { key, sec ->
                [(key): sec + [content: this.convertImages(sec.content)]]
            }
        }

        if (!sections) {
            throw new RuntimeException("Error: unable to create ${documentType}. " +
                'Could not obtain document chapter data from Jira nor files.')
        }
        // Extract-out the section, as needed for the DocGen interface
        return sections
    }

    /**
     * gets teh document version IDS at the start ... can't do that...
     * @return Map
     */
    private Map getReferencedDocumentsVersion(ProjectData projectData) {
        if (!this.jiraUseCase) return [:]
        if (!this.jiraUseCase.jira) return [:]

        Constants.referencedDcocs.collectEntries { dt ->
            def doc = dt as String
            def version = getVersion(projectData, doc)

            return [(doc): "${projectData.build.configItem} / ${version}"]
        }
    }

    private String getVersion(ProjectData projectData, String doc) {
        String version = getVersionFromDocuments(projectData, doc)
        if (projectData.isWorkInProgress) {
            // If this is a developer preview, the document version is always a WIP, because,
            // if we have the document history, it has already been updated to a new version.
            return  "${version}-WIP"
        }
        return version
    }

    private String getVersionFromDocuments(ProjectData projectData, String doc) {
        String version
        if (projectData.isVersioningEnabled) {
            version = projectData.getDocumentVersionFromHistories(doc)
            if (!version) {
                // The document has not (yet) been generated in this pipeline run.
                def envs = Environment.values().collect { it.toString() }
                def trackingIssues = this.getDocumentTrackingIssuesForHistory(projectData, doc, envs)
                version = this.jiraUseCase.getLatestDocVersionId(projectData, trackingIssues)
                if (projectData.isWorkInProgress ||
                        LeVADocumentScheduler.getFirstCreationEnvironment(doc) == //TODO s2o see what we do
                        projectData.build.targetEnvironmentToken) {
                    // Either this is a developer preview or the history is to be updated in this environment.
                    version += 1L
                }
            }
        } else {
            // TODO removeme in ODS 4.x
            version = "${projectData.build.version}-${projectData.build.buildNumber}"
        }
        return version
    }


    private def computeKeysInDocForTCR(def data) {
        return data.collect { it.subMap(['key', 'requirements', 'bugs']).values() }.flatten()
    }
    private String replaceDashToNonBreakableUnicode(theString) {
        return theString?.replaceAll('-', '&#x2011;')
    }

    private def getReqsWithNoGampTopic(def requirements) {
        return requirements.findAll { it.gampTopic == null }
    }

    private def getReqsGroupedByGampTopic(def requirements) {
        return requirements.findAll { it.gampTopic != null }
                .groupBy { it.gampTopic.toLowerCase() }
    }

    private Map sortByEpicAndRequirementKeys(List updatedReqs) {
        def sortedUpdatedReqs = SortUtil.sortIssuesByKey(updatedReqs)
        def reqsGroupByEpic = sortedUpdatedReqs.findAll {
            it.epic != null }.groupBy { it.epic }.sort()

        def reqsGroupByEpicUpdated = reqsGroupByEpic.values().indexed(1).collect { index, epicStories ->
            def aStory = epicStories.first()
            [
                    epicName        : aStory.epicName,
                    epicTitle       : aStory.epicTitle,
                    epicDescription : this.convertImages(aStory.epicDescription ?: ''),
                    key             : aStory.epic,
                    epicIndex       : index,
                    stories         : epicStories,
            ]
        }
        def output = [
                noepics: sortedUpdatedReqs.findAll { it.epic == null },
                epics  : reqsGroupByEpicUpdated
        ]

        return output
    }

    private  List<String> computeKeysInDocForCSD(def data) {
        return data.collect { it.subMap(['key', 'epics']).values() }.flatten().unique() as List<String>
    }

    private def computeKeysInDocForDTP(def data, def tests) {
        return data.collect { 'Technology-' + it.id } + tests
                .collect { [it.testKey, it.systemRequirement.split(', '), it.softwareDesignSpec.split(', ')]  }
                .flatten()
    }

}
