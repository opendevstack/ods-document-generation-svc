package org.ods.doc.gen.project.data

import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import groovy.util.logging.Slf4j
import org.ods.doc.gen.external.modules.git.GitRepoDownloadService
import org.ods.doc.gen.external.modules.jira.CustomIssueFields
import org.ods.doc.gen.external.modules.jira.IssueTypes
import org.ods.doc.gen.external.modules.jira.JiraService
import org.ods.doc.gen.external.modules.jira.LabelPrefix
import org.ods.doc.gen.external.modules.jira.OpenIssuesException
import org.ods.doc.gen.leva.doc.services.DocumentHistory
import org.ods.doc.gen.leva.doc.services.LeVADocumentUtil
import org.ods.doc.gen.leva.doc.services.PipelineConfig
import org.springframework.stereotype.Service
import org.yaml.snakeyaml.Yaml

import java.nio.file.NoSuchFileException
import java.nio.file.Paths

@SuppressWarnings(['LineLength',
        'AbcMetric',
        'IfStatementBraces',
        'Instanceof',
        'CyclomaticComplexity',
        'GStringAsMapKey',
        'ImplementationAsType',
        'UseCollectMany',
        'MethodCount',
        'PublicMethodsBeforeNonPublicMethods'])
@Slf4j
@Service
class ProjectData {

    protected static final String BUILD_PARAM_VERSION_DEFAULT = 'WIP'
    protected static final String METADATA_FILE_NAME = 'metadata.yml'
    protected static final String BASE_DIR = 'projectData'

    protected Map config
    protected Boolean isVersioningEnabled = false

    private final JiraService jira
    private final GitRepoDownloadService gitRepoDownloadService

    String tmpFolder
    Map data = [:]
    Map build = [:]

    ProjectData(JiraService jira, GitRepoDownloadService gitRepoDownloadService) {
        this.jira = jira
        this.gitRepoDownloadService = gitRepoDownloadService

        this.config =  [:]
        this.build = [
            hasFailingTests: false,
            hasUnexecutedJiraTests: false,
        ]
        this.data.documentHistories = [:]
    }

    ProjectData init(Map data) {
        this.tmpFolder = data.tmpFolder
        this.build << data.build
        this.build.buildNumber = data.buildNumber
        this.data.git = data.git
        this.data.openshift = data.openshift
        this.data.documents = [:]
        this.data.jira = [project: [ : ]]
        return this
    }

    ProjectData load() {

        gitRepoDownloadService.getRepoContentsToFolder(data, tmpFolder)
        this.data.metadata = loadMetadata(tmpFolder) // TODO s2o load from BB
        this.data.jira.issueTypes = this.loadJiraDataIssueTypes()
        this.data.jira << this.loadJiraData(this.jiraProjectKey)

        // Get more info of the versions from Jira
        this.data.jira.project.version = this.loadCurrentVersionDataFromJira()
        def version = this.data.jira.version

        // FIXME: contrary to the comment below, the bug data from this method is still relevant
        // implementation needs to be cleaned up and bug data should be delivered through plugin's
        // REST endpoint, not plain Jira
        this.data.jira.bugs = this.loadJiraDataBugs(this.data.jira.tests, version) // TODO removeme when endpoint is updated
        this.data.jira = this.convertJiraDataToJiraDataItems(this.data.jira)
        this.data.jiraResolved = this.resolveJiraDataItemReferences(this.data.jira)

        this.data.jira.trackingDocs = this.loadJiraDataTrackingDocs(version)
        this.data.jira.trackingDocsForHistory = this.loadJiraDataTrackingDocs()
        this.data.jira.undone = this.computeWipJiraIssues(this.data.jira)
        this.data.jira.undoneDocChapters = this.computeWipDocChapterPerDocument(this.data.jira)

        if (this.hasWipJiraIssues()) {
            String message = ProjectMessagesUtil.generateWIPIssuesMessage(this)

            if(!this.isWorkInProgress){
                throw new OpenIssuesException(message)
            }
            this.addCommentInReleaseStatus(message)
        }

        log.debug("Verify that each unit test in Jira project ${this.key} has exactly one component assigned.")
        def faultMap = [:]
        this.data.jira.tests
                .findAll { it.value.get("testType") == "Unit" }
                .each { entry ->
                    if(entry.value.get("components").size() != 1) {
                        faultMap.put(entry.key, entry.value.get("components").size())
                    }
                }
        if(faultMap.size() != 0) {
            def faultyTestIssues = faultMap.keySet()
                    .collect { key -> key + ": " + faultMap.get(key) + "; " }
                    .inject("") { temp, val -> temp + val }
            throw new IllegalArgumentException("Error: unit tests must have exactly 1 component assigned. Following unit tests have an invalid number of components: ${faultyTestIssues}")
        }

        this.updateJiraReleaseStatusBuildNumber()
        return this
    }


    private Map loadMetadata(String workspace){
        Map result = parseMetadataFile(workspace)
        result.description = (result.description)?: ""
        result.repositories = (result.repositories)?: ""
        updateRepositories(result)
        result.capabilities = (result.capabilities )?: []
        updateLevaDocCapability(result)
        result.environments = (result.environments)?: [:]
        return result
    }

    private void updateLevaDocCapability(Map result) {
        def levaDocsCapabilities = result.capabilities.findAll { it instanceof Map && it.containsKey('LeVADocs') }
        if (levaDocsCapabilities) {
            if (levaDocsCapabilities.size() > 1) {
                throw new IllegalArgumentException(
                        "Error: unable to parse project metadata. More than one 'LeVADoc' capability has been defined.")
            }

            def levaDocsCapability = levaDocsCapabilities.first()

            def gampCategory = levaDocsCapability.LeVADocs?.GAMPCategory
            if (!gampCategory) {
                throw new IllegalArgumentException(
                        "Error: 'LeVADocs' capability has been defined but contains no 'GAMPCategory'.")
            }

            def templatesVersion = levaDocsCapability.LeVADocs?.templatesVersion
            if (!templatesVersion) {
                levaDocsCapability.LeVADocs.templatesVersion = DEFAULT_TEMPLATE_VERSION
            }
        }
    }

    private void updateRepositories(Map result) {
        result.repositories.eachWithIndex { repo, index ->
            // Check for existence of required attribute 'repositories[i].id'
            if (!repo.id?.trim()) {
                throw new IllegalArgumentException(
                        "Error: unable to parse project meta data. Required attribute 'repositories[${index}].id' is undefined.")
            }

            repo.data = [
                    openshift: [:],
                    documents: [:],
            ]

            // Set repo type, if not provided
            if (!repo.type?.trim()) {
                repo.type = PipelineConfig.REPO_TYPE_ODS_CODE
            }

            repo.url = "gitURL getGitURLFromPath"
            repo.branch = 'master'
            repo.metadata = loadMetadataRepo(repo)
        }
    }

    private Map<String, String> loadMetadataRepo(repo) {
        return  [
                id: repo.id,
                name: repo.name,
                description: "myDescription-A",
                supplier: "mySupplier-A",
                version: "myVersion-A",
                references: "myReferences-A"
        ]
    }

    private Map parseMetadataFile(String workspace) {
        String filename = METADATA_FILE_NAME
        def file = Paths.get(workspace, filename).toFile()
        if (!file.exists()) {
            throw new RuntimeException("Error: unable to load project meta data. File '${workspace}/${filename}' does not exist.")
        }

        Map result = new Yaml().load(file.text)

        // Check for existence of required attribute 'id'
        if (!result?.id?.trim()) {
            throw new IllegalArgumentException("Error: unable to parse project meta data. Required attribute 'id' is undefined.")
        }

        // Check for existence of required attribute 'name'
        if (!result?.name?.trim()) {
            throw new IllegalArgumentException("Error: unable to parse project meta data. Required attribute 'name' is undefined.")
        }
        return result
    }

    void updateJiraReleaseStatusBuildNumber() {
        String releaseStatusIssueKey = buildParams.releaseStatusJiraIssueKey
        Map releaseStatusIssueFields = getJiraFieldsForIssueType(IssueTypes.RELEASE_STATUS)
        Map releaseStatusIssueBuildNumberField = releaseStatusIssueFields['Release Build']
        Map testFields = [(releaseStatusIssueBuildNumberField.id): "${buildParams.version}-${build.buildNumber}"]
        this.jira.updateTextFieldsOnIssue(releaseStatusIssueKey, testFields)
    }

    Map<String, List> getWipJiraIssues() {
        return this.data.jira.undone
    }

    
    boolean hasWipJiraIssues() {
        def values = this.getWipJiraIssues().values()
        values = values.collect { it instanceof Map ? it.values() : it }.flatten()
        return !values.isEmpty()
    }

    boolean getIsAssembleMode() {
        !getIsPromotionMode()
    }

    boolean getIsPromotionMode() {
        isPromotionMode(buildParams.targetEnvironmentToken)
    }

    static boolean isPromotionMode(String targetEnvironmentToken) {
        ['Q', 'P'].contains(targetEnvironmentToken)
    }

    protected Map<String, List> computeWipJiraIssues(Map data) {
        def result = [:]
        JiraDataItem.TYPES_WITH_STATUS.each { type ->
            if (data.containsKey(type)) {
                result[type] = data[type].findAll { k, v -> issueIsWIP(v) }.keySet() as List<String>
            }
        }
        return result
    }

    /**
     * Gets the document chapter issues and puts in a format ready to query from levadocumentusecase when retrieving
     * the sections not done
     * @param data jira data
     * @return dict with map documentTypes -> sectionsNotDoneKeys
     */
    
    protected Map<String,List> computeWipDocChapterPerDocument(Map data) {
        (data[JiraDataItem.TYPE_DOCS] ?: [:])
            .values()
            .findAll { issueIsWIP(it) }
            .collect { chapter ->
                chapter.documents.collect { [doc: it, key: chapter.key] }
            }.flatten()
            .groupBy { it.doc }
            .collectEntries { doc, issues ->
                [(doc as String): issues.collect { it.key } as List<String>]
            }
    }

    
    protected boolean issueIsWIP(Map issue) {
        issue.status != null &&
            !issue.status.equalsIgnoreCase(JiraDataItem.ISSUE_STATUS_DONE) &&
            !issue.status.equalsIgnoreCase(JiraDataItem.ISSUE_STATUS_CANCELLED)
    }

    
    protected Map convertJiraDataToJiraDataItems(Map data) {
        JiraDataItem.TYPES.each { type ->
            if (data.containsKey(type)) {
                data[type] = data[type].collectEntries { key, item ->
                    [key, new JiraDataItem(this, item, type)]
                }
            } //else {
                //throw new IllegalArgumentException(
                //    "Error: Jira data does not include references to items of type '${type}'.")
            //}
        }

        return data
    }

    
    List<JiraDataItem> getAutomatedTests(String componentName = null, List<String> testTypes = []) {
        return this.data.jira.tests.findAll { key, testIssue ->
            return isAutomatedTest(testIssue) && hasGivenTypes(testTypes, testIssue) && hasGivenComponent(testIssue, componentName)
        }.values() as List
    }

    
    boolean isAutomatedTest(testIssue) {
        testIssue.executionType?.toLowerCase() == JiraDataItem.ISSUE_TEST_EXECUTION_TYPE_AUTOMATED
    }

    
    boolean hasGivenTypes(List<String> testTypes, testIssue) {
        def result = true
        if (testTypes) {
            result = testTypes*.toLowerCase().contains(testIssue.testType.toLowerCase())
        }
        return result
    }

    
    boolean hasGivenComponent(testIssue, String componentName) {
        def result = true
        if (componentName) {
            result = testIssue.getResolvedComponents().collect {
                if (!it || !it.name) {
                    throw new RuntimeException("Error with testIssue key: ${testIssue.key}, no component assigned or it is wrong.")
                }
                it.name.toLowerCase()
            }.contains(componentName.toLowerCase())
        }
        return result
    }

    
    Map getEnumDictionary(String name) {
        return this.data.jira.project.enumDictionary[name]
    }

    Map getProjectProperties() {
        return this.data.jira.project.projectProperties
    }

    
    List<JiraDataItem> getAutomatedTestsTypeAcceptance(String componentName = null) {
        return this.getAutomatedTests(componentName, [TestType.ACCEPTANCE])
    }

    
    List<JiraDataItem> getAutomatedTestsTypeInstallation(String componentName = null) {
        return this.getAutomatedTests(componentName, [TestType.INSTALLATION])
    }

    
    List<JiraDataItem> getAutomatedTestsTypeIntegration(String componentName = null) {
        return this.getAutomatedTests(componentName, [TestType.INTEGRATION])
    }

    
    List<JiraDataItem> getAutomatedTestsTypeUnit(String componentName = null) {
        return this.getAutomatedTests(componentName, [TestType.UNIT])
    }


    boolean getIsVersioningEnabled() {
        isVersioningEnabled
    }

    boolean getIsWorkInProgress() {
        isWorkInProgress(buildParams.version)
    }

    boolean isDeveloperPreviewMode() {
        return BUILD_PARAM_VERSION_DEFAULT.equalsIgnoreCase(this.build.version) &&
                this.build.targetEnvironmentToken == "D"
    }

    static boolean isWorkInProgress(String version) {
        version == BUILD_PARAM_VERSION_DEFAULT
    }

    Map getBuildParams() {
        return this.build
    }

    List getCapabilities() {
        return this.data.metadata.capabilities
    }

    
    Object getCapability(String name) {
        def entry = this.getCapabilities().find { it instanceof Map ? it.find { it.key == name } : it == name }
        if (entry) {
            return entry instanceof Map ? entry[name] : true
        }

        return null
    }

    
    List<JiraDataItem> getBugs() {
        return this.data.jira.bugs.values() as List
    }

    
    List<JiraDataItem> getComponents() {
        return this.data.jira.components.values() as List
    }

    
    String getDescription() {
        return this.data.metadata.description
    }

    
    List<Map> getDocumentTrackingIssues() {
        return this.data.jira.trackingDocs.values() as List
    }

    
    List<Map> getDocumentTrackingIssues(List<String> labels) {
        def result = []

        def issues = this.getDocumentTrackingIssues()
        labels.each { label ->
            issues.each { issue ->
                if (issue.labels.collect { it.toLowerCase() }.contains(label.toLowerCase())) {
                    result << [key: issue.key, status: issue.status]
                }
            }
        }

        return result.unique()
    }

    
    List<Map> getDocumentTrackingIssuesForHistory() {
        return this.data.jira.trackingDocsForHistory.values() as List
    }

    
    List<Map> getDocumentTrackingIssuesForHistory(List<String> labels) {
        def result = []

        def issues = this.getDocumentTrackingIssuesForHistory()
        labels.each { label ->
            issues.each { issue ->
                if (issue.labels.collect { it.toLowerCase() }.contains(label.toLowerCase())) {
                    result << [key: issue.key, status: issue.status]
                }
            }
        }

        return result.unique()
    }

    Map getGitData() {
        return this.data.git
    }
    
    Map<String, DocumentHistory> getDocumentHistories() {
        return this.data.documentHistories
    }

    /**
     * Obtains the mapping of Jira fields for a given issue type from the saved data
     * @param issueTypeName Jira issue type
     * @return Map containing [id: "customfield_XYZ", name:"name shown in jira"]
     */
    Map getJiraFieldsForIssueType(String issueTypeName) {
        return this.data.jira?.issueTypes[issueTypeName]?.fields ?: [:]
    }

    String getKey() {
        return this.data.metadata.id
    }

    String getJiraProjectKey() {
        def services = this.getServices()
        if (services?.jira?.project) {
            return services.jira.project
        }

        return getKey()
    }

    List<JiraDataItem> getMitigations() {
        return this.data.jira.mitigations.values() as List
    }

    String getName() {
        return this.data.metadata.name
    }

    
    List<Map> getRepositories() {
        return this.data.metadata.repositories
    }

    List<JiraDataItem> getRequirements() {
        return this.data.jira.requirements.values() as List
    }
    
    List<JiraDataItem> getRisks() {
        return this.data.jira.risks.values() as List
    }

    Map getServices() {
        return this.data.metadata.services
    }
    
    List<JiraDataItem> getSystemRequirements(String componentName = null, List<String> gampTopics = []) {
        return this.data.jira.requirements.findAll { key, req ->
            def result = true

            if (result && componentName) {
                result = req.getResolvedComponents().collect { it.name.toLowerCase() }.
                        contains(componentName.toLowerCase())
            }

            if (result && gampTopics) {
                result = gampTopics.collect { it.toLowerCase() }.contains(req.gampTopic.toLowerCase())
            }

            return result
        }.values() as List
    }

    List<JiraDataItem> getTechnicalSpecifications(String componentName = null) {
        return this.data.jira.techSpecs.findAll { key, techSpec ->
            def result = true

            if (result && componentName) {
                result = techSpec.getResolvedComponents().collect { it.name.toLowerCase() }.
                        contains(componentName.toLowerCase())
            }

            return result
        }.values() as List
    }

    List<JiraDataItem> getDocumentChaptersForDocument(String document) {
        def docs = this.data.jira[JiraDataItem.TYPE_DOCS] ?: [:]
        return docs.findAll { k, v -> v.documents && v.documents.contains(document) }.values() as List
    }

    List<String> getWIPDocChaptersForDocument(String documentType) {
        def docs = this.getWIPDocChapters()
        return docs[documentType] ?: []
    }

    Map getWIPDocChapters() {
        return this.data.jira.undoneDocChapters ?: [:]
    }

    // Deprecated in favour of getOpenShiftTargetApiUrl
    String getOpenShiftApiUrl() {
        this.data.openshift.targetApiUrl
    }

    boolean historyForDocumentExists(String document) {
        return this.getHistoryForDocument(document) ? true : false
    }

    DocumentHistory getHistoryForDocument(String document) {
        return this.documentHistories[document]
    }

    Long getDocumentVersionFromHistories(String documentType) {
        def history = getHistoryForDocument(documentType)
        if (!history) {
            // All docHistories for DTR and TIR should have the same version
            history = this.documentHistories.find {
                LeVADocumentUtil.getTypeFromName(it.key) == documentType
            }?.value
        }
        return history?.version
    }

    void setHistoryForDocument(DocumentHistory docHistory, String document) {
        this.documentHistories[document] = docHistory
    }

    protected Map loadJiraData(String projectKey) {
        // FIXME: getVersionFromReleaseStatusIssue loads data from Jira and should therefore be called not more
        // than once. However, it's also called via this.project.versionFromReleaseStatusIssue in JiraUseCase.groovy.
        def currentVersion = this.getVersionFromReleaseStatusIssue() // TODO why is param.version not sufficient here?
        return this.loadJiraDataForCurrentVersion(projectKey, currentVersion)
    }

    String getVersionFromReleaseStatusIssue() {
        def releaseStatusIssueKey = buildParams.releaseStatusJiraIssueKey as String
        def releaseStatusIssueFields = getJiraFieldsForIssueType(IssueTypes.RELEASE_STATUS)

        def productReleaseVersionField = releaseStatusIssueFields[CustomIssueFields.RELEASE_VERSION]
        def versionField = jira.getTextFieldsOfIssue(releaseStatusIssueKey, [productReleaseVersionField.id])
        if (!versionField || !versionField[productReleaseVersionField.id]?.name) {
            throw new IllegalArgumentException('Unable to obtain version name from release status issue' +
                    " ${releaseStatusIssueKey}. Please check that field with name" +
                    " '${productReleaseVersionField.name}' and id '${productReleaseVersionField.id}' " +
                    'has a correct version value.')
        }

        return versionField[productReleaseVersionField.id].name
    }

    protected Map loadVersionJiraData(String projectKey, String versionName) {
        def result = jira.getDeltaDocGenData(projectKey, versionName)
        if (result?.project?.id == null) {
            throw new IllegalArgumentException(
                "Error: unable to load documentation generation data from Jira. 'project.id' is undefined.")
        }

        def docChapterData = this.getDocumentChapterData(projectKey, versionName)
        result << [(JiraDataItem.TYPE_DOCS as String): docChapterData]
        return result
    }

    /**
     * Obtains all document chapter data attached attached to a given version
     * @param versionName the version name from jira
     * @return Map (key: issue) with all the document chapter issues and its relevant content
     */
    @SuppressWarnings(['AbcMetric'])
    protected Map<String, Map> getDocumentChapterData(String projectKey, String versionName = null) {
        def docChapterIssueFields = getJiraFieldsForIssueType(IssueTypes.DOCUMENTATION_CHAPTER)
        def contentField = docChapterIssueFields[CustomIssueFields.CONTENT].id
        def headingNumberField = docChapterIssueFields[CustomIssueFields.HEADING_NUMBER].id

        def jql = "project = ${projectKey} " +
                "AND issuetype = '${IssueTypes.DOCUMENTATION_CHAPTER}'"

        if (versionName) {
            jql = jql + " AND fixVersion = '${versionName}'"
        }

        def jqlQuery = [
                fields: ['key', 'status', 'summary', 'labels', 'issuelinks', contentField, headingNumberField],
                jql: jql,
                expand: ['renderedFields'],
        ]

        def result = this.jira.searchByJQLQuery(jqlQuery)
        if (!result || result.total == 0) {
            this.log.warn("There are no document chapters assigned to this version. Using JQL query: '${jqlQuery}'.")
            return [:]
        }

        return result.issues.collectEntries { issue ->
            def number = issue.fields.find { field ->
                headingNumberField == field.key && field.value
            }
            if (!number) {
                throw new IllegalArgumentException("Error: could not find heading number for issue '${issue.key}'.")
            }
            number = number.getValue().trim()

            def content = issue.renderedFields.find { field ->
                contentField == field.key && field.value
            }
            content = content ? content.getValue() : ""

            this.thumbnailImageReplacement(content)

            def documentTypes = (issue.fields.labels ?: [])
                    .findAll { String l -> l.startsWith(LabelPrefix.DOCUMENT) }
                    .collect { String l -> l.replace(LabelPrefix.DOCUMENT, '') }
            if (documentTypes.size() == 0) {
                throw new IllegalArgumentException("Error: issue '${issue.key}' of type " +
                        "'${IssueTypes.DOCUMENTATION_CHAPTER}' contains no " +
                        "document labels. There should be at least one label starting with '${LabelPrefix.DOCUMENT}'")
            }

            def predecessorLinks = issue.fields.issuelinks
                    .findAll { it.type.name == "Succeeds" && it.outwardIssue?.key }
                    .collect { it.outwardIssue.key }

            return [(issue.key as String): [
                    section: "sec${number.replaceAll(/\./, "s")}".toString(),
                    number: number,
                    heading: issue.fields.summary,
                    documents: documentTypes,
                    content: content?.replaceAll("\u00a0", " ") ?: " ",
                    status: issue.fields.status.name,
                    key: issue.key as String,
                    predecessors: predecessorLinks.isEmpty()? [] : predecessorLinks,
                    versions: versionName? [versionName] : [],
            ]
            ]
        }
    }

    protected Map loadJiraDataForCurrentVersion(String projectKey, String versionName) {
        def result = [:]
        def newData = this.loadVersionJiraData(projectKey, versionName)

        // Get more info of the versions from Jira
        def predecessors = newData.precedingVersions ?: []
        def previousVersionId = null
        if (predecessors && ! predecessors.isEmpty()) {
            previousVersionId = predecessors.first()
        }

        if (previousVersionId) {
            log.info("Found a predecessor project version with ID '${previousVersionId}'. Loading its data.")
            def savedDataFromOldVersion = this.loadSavedJiraData(previousVersionId)
            def mergedData = this.mergeJiraData(savedDataFromOldVersion, newData)
            result << this.addKeyAndVersionToComponentsWithout(mergedData)
            result.previousVersion = previousVersionId
        } else {
            log.info("No predecessor project version found. Loading only data from Jira.")
            result << this.addKeyAndVersionToComponentsWithout(newData)
        }

        // Get more info of the versions from Jira
        result.project << [previousVersion: this.loadVersionDataFromJira(previousVersionId)]

        return result
    }

    protected Map loadJiraDataBugs(Map tests, String versionName = null) {
        def fields = ['assignee', 'duedate', 'issuelinks', 'status', 'summary']
        def jql = "project = ${this.jiraProjectKey} AND issuetype = Bug AND status != Done"

        if (versionName) {
            fields << 'fixVersions'
            jql = jql + " AND fixVersion = '${versionName}'"
        }

        def jqlQuery = [
            fields: fields,
            jql: jql,
            expand: []
        ]

        def jiraBugs = jira.getIssuesForJQLQuery(jqlQuery) ?: []

        return jiraBugs.collectEntries { jiraBug ->
            def bug = [
                key: jiraBug.key,
                name: jiraBug.fields.summary,
                assignee: jiraBug.fields.assignee ? [jiraBug.fields.assignee.displayName, jiraBug.fields.assignee.name, jiraBug.fields.assignee.emailAddress].find { it != null } : "Unassigned",
                dueDate: '', // TODO: currently unsupported for not being enabled on a Bug issue
                status: jiraBug.fields.status.name,
                versions: jiraBug.fields.fixVersions.collect { it.name }
            ]

            def testKeys = []
            if (jiraBug.fields.issuelinks) {
                testKeys = jiraBug.fields.issuelinks.findAll {
                    it.type.name == 'Blocks' && it.outwardIssue &&
                            it.outwardIssue.fields.issuetype.name == 'Test'
                }.collect { it.outwardIssue.key }
            }

            // Add relations from bug to tests
            bug.tests = testKeys

            // Add relations from tests to bug
            testKeys.each { testKey ->
                if (!tests[testKey].bugs) {
                    tests[testKey].bugs = []
                }

                tests[testKey].bugs << bug.key
            }

            return [jiraBug.key, bug]
        }
    }

    protected Map loadCurrentVersionDataFromJira() {
        loadVersionDataFromJira(this.buildParams.version)
    }

    Map loadVersionDataFromJira(String versionName) {
        return jira.getVersionsForProject(this.jiraProjectKey).find { version ->
            versionName == version.name
        }
    }

    protected Map loadJiraDataTrackingDocs(String versionName = null) {
        def jql = "project = ${this.jiraProjectKey} AND issuetype = '${IssueTypes.DOCUMENTATION_TRACKING}'"

        if (versionName) {
            jql = jql + " AND fixVersion = '${versionName}'"
        }

        def jqlQuery = [
            jql: jql
        ]

        def jiraIssues = jira.getIssuesForJQLQuery(jqlQuery)
        if (jiraIssues.isEmpty()) {
            def message = "Error: Jira data does not include references to items of type '${JiraDataItem.TYPE_DOCTRACKING}'"
            if (versionName) {
                message += " for version '${versionName}'"
            }
            message += "."

            throw new IllegalArgumentException(message)
        }

        return jiraIssues.collectEntries { jiraIssue ->
            [
                jiraIssue.key,
                [
                    key: jiraIssue.key,
                    name: jiraIssue.fields.summary,
                    description: jiraIssue.fields.description,
                    status: jiraIssue.fields.status.name,
                    labels: jiraIssue.fields.labels,
                ],
            ]
        }
    }

    protected Map loadJiraDataIssueTypes() {
        def jiraIssueTypes = jira.getIssueTypes(this.jiraProjectKey)
        return jiraIssueTypes.values.collectEntries { jiraIssueType ->
            [
                jiraIssueType.name,
                [
                    id: jiraIssueType.id,
                    name: jiraIssueType.name,
                    fields: jira.getIssueTypeMetadata(this.jiraProjectKey, jiraIssueType.id).values.collectEntries { value ->
                        [
                            value.name,
                            [
                                id:   value.fieldId,
                                name: value.name,
                            ]
                        ]
                    },
                ]
            ]
        }
    }

    void addCommentInReleaseStatus(String message) {
        def releaseStatusIssueKey = buildParams.releaseStatusJiraIssueKey
        if (message) {
            this.jira.appendCommentToIssue(releaseStatusIssueKey, "${message}\n\nSee: ${build.runDisplayUrl}")
        }

    }

    protected Map resolveJiraDataItemReferences(Map data) {
        this.resolveJiraDataItemReferences(data, JiraDataItem.TYPES)
    }

    protected Map resolveJiraDataItemReferences(Map data, List<String> jiraTypes) {
        def result = [:]

        data.each { type, values ->
            if (!jiraTypes.contains(type)) {
                return
            }

            result[type] = [:]

            values.each { key, item ->
                result[type][key] = [:]

                jiraTypes.each { referenceType ->
                    if (item.containsKey(referenceType)) {
                        result[type][key][referenceType] = []

                        item[referenceType].eachWithIndex { referenceKey, index ->
                            result[type][key][referenceType][index] = data[referenceType][referenceKey]
                        }
                    }
                }
            }
        }

        return result
    }

    String toString() {
        // Don't serialize resolved Jira data items
        def result = this.data.subMap(['build', 'buildParams', 'metadata', 'git', 'jira'])

        if (!services?.jira && capabilities?.empty) {
            result.remove('jira')
        }

        return JsonOutput.prettyPrint(JsonOutput.toJson(result))
    }

    Object loadSavedJiraData(String savedVersion) {
        String fileName = "${BASE_DIR}/${savedVersion}.json"
        try {
            String savedData =  new File("${tmpFolder}/${fileName}")?.text
            return new JsonSlurperClassic().parseText(savedData) ?: [:]
        } catch (NoSuchFileException e) {
            throw new NoSuchFileException("File '${fileName}' is expected to be inside the release " +
                    'manager repository but was not found and thus, document history cannot be build. If you come from ' +
                    'and old ODS version, create one for each document to use the automated document history feature.')
        } catch (RuntimeException ex) {
            throw new RuntimeException("Error parsing File '${fileName}'", ex)
        }
    }

    Map mergeJiraData(Map oldData, Map newData) {
        def mergeMaps = { Map left, Map right ->
            def keys = (left.keySet() + right.keySet()).toSet()
            keys.collectEntries { key ->
                if (JiraDataItem.TYPES.contains(key)) {
                    if (!left[key] || left[key].isEmpty()) {
                        [(key): right[key]]
                    } else if (!right[key] || right[key].isEmpty()) {
                        [(key): left[key]]
                    } else {
                        [(key): left[key] + right[key]]
                    }
                } else {
                    [(key): right[key]]
                }
            }
        }

        // Here we update the existing links in 3 ways:
        // - Deleting links of removing issues
        // - Adding links to new issues
        // - Updating links for changes in issues (changing key 1 for key 2)
        def updateIssues = { Map<String,Map> left, Map<String,Map> right ->
            def updateLink = { String issueType, String issueToUpdateKey, Map link ->
                if (! left[issueType][issueToUpdateKey][link.linkType]) {
                    left[issueType][issueToUpdateKey][link.linkType] = []
                }
                if (link.action == 'add') {
                    left[issueType][issueToUpdateKey][link.linkType] << link.origin
                } else if (link.action == 'discontinue') {
                    left[issueType][issueToUpdateKey][link.linkType].removeAll { it == link.origin }
                } else if (link.action == 'change') {
                    left[issueType][issueToUpdateKey][link.linkType] << link.origin
                    left[issueType][issueToUpdateKey][link.linkType].removeAll { it == link."replaces" }
                }
                // Remove potential duplicates in place
                left[issueType][issueToUpdateKey][link.linkType].unique(true)
            }

            def reverseLinkIndex = buildChangesInLinks(left, right)
            left.findAll { JiraDataItem.TYPES.contains(it.key) }.each { issueType, issues ->
                issues.values().each { Map issueToUpdate ->
                    def linksToUpdate = reverseLinkIndex[issueToUpdate.key] ?: []
                    linksToUpdate.each { Map link ->
                        try {
                            updateLink(issueType, issueToUpdate.key, link)
                        } catch (Exception e) {
                            throw new IllegalStateException("Error found when updating link ${link} for issue " +
                                "${issueToUpdate.key} from a previous version. Error message: ${e.message}", e)
                        }
                    }
                }
            }
            return left
        }

        def updateIssueLinks = { issue, index ->
            issue.collectEntries { String type, value ->
                if(JiraDataItem.TYPES.contains(type)) {
                    def newLinks = value.collect { link ->
                        def newLink = index[link]
                        newLink?:link
                    }.unique()
                    [(type): newLinks]
                } else {
                    [(type): value]
                }
            }
        }

        def updateLinks = { data, index ->
            data.collectEntries { issueType, content ->
                if(JiraDataItem.TYPES.contains(issueType)) {
                    def updatedIssues = content.collectEntries { String issueKey, Map issue ->
                        def updatedIssue = updateIssueLinks(issue, index)
                        [(issueKey): updatedIssue]
                    }
                    [(issueType): updatedIssues]
                } else {
                    [(issueType): content]
                }
            }
        }

        if (!oldData || oldData.isEmpty()) {
            newData
        } else {
            oldData[JiraDataItem.TYPE_COMPONENTS] = this.mergeComponentsLinks(oldData, newData)
            def discontinuations = (newData.discontinuedKeys ?: []) +
                this.getComponentDiscontinuations(oldData, newData)
            newData.discontinuations = discontinuations
            // Expand some information from old saved data
            def newDataExpanded = expandPredecessorInformation (oldData, newData, discontinuations)
            newDataExpanded << [discontinuationsPerType: discontinuationsPerType(oldData, discontinuations)]

            // Update data from previous version
            def oldDataWithUpdatedLinks = updateIssues(oldData, newDataExpanded)
            def successorIndex = getSuccessorIndex(newDataExpanded)
            def newDataExpandedWithUpdatedLinks = updateLinks(newDataExpanded, successorIndex)
            def obsoleteKeys = discontinuations + successorIndex.keySet()
            def oldDataWithoutObsoletes = removeObsoleteIssues(oldDataWithUpdatedLinks, obsoleteKeys)

            // merge old component data to new for the existing components
            newDataExpandedWithUpdatedLinks[JiraDataItem.TYPE_COMPONENTS] = newDataExpandedWithUpdatedLinks[JiraDataItem.TYPE_COMPONENTS]
                .collectEntries { compN, v ->
                    [ (compN): (oldDataWithoutObsoletes[JiraDataItem.TYPE_COMPONENTS][compN] ?: v)]
                }
            mergeMaps(oldDataWithoutObsoletes, newDataExpandedWithUpdatedLinks)
        }
    }

    /**
     * Return old components with the links coming from the new data. This is because we are not receiving all
     * the old links from the docgen reports for the components. and we need a special merge.
     * @param oldComponents components of the saved data
     * @param newComponents components for the new data
     * @return merged components with all the links
     */
    
    private Map mergeComponentsLinks(Map oldComponents, Map newComponents) {
        oldComponents[JiraDataItem.TYPE_COMPONENTS].collectEntries { compName, oldComp ->
            def newComp = newComponents[JiraDataItem.TYPE_COMPONENTS][compName] ?: [:]
            def updatedComp = mergeJiraItemLinks(oldComp, newComp)
            [(compName): updatedComp]
        }
    }

    
    private static mergeJiraItemLinks(Map oldItem, Map newItem, List discontinuations = []) {
        Map oldItemWithCurrentLinks = oldItem.collectEntries { key, value ->
            if (JiraDataItem.TYPES.contains(key)) {
                [(key): value - discontinuations]
            } else {
                [(key): value]
            }
        }.findAll { key, value ->
            !JiraDataItem.TYPES.contains(key) || value
        }
        (oldItemWithCurrentLinks.keySet() + newItem.keySet()).collectEntries { String type ->
            if (JiraDataItem.TYPES.contains(type)) {
                [(type): ((newItem[type] ?: []) + (oldItemWithCurrentLinks[type] ?: [])).unique()]
            } else {
                [(type): newItem[type] ?: oldItemWithCurrentLinks[type]]
            }
        }
    }

    private Map<String, List<String>> discontinuationsPerType (Map savedData, List<String> discontinuations) {
        savedData.findAll { JiraDataItem.TYPES.contains(it.key) }
            .collectEntries { String issueType, Map issues ->
                def discontinuationsPerType = issues.values().findAll { discontinuations.contains(it.key) }
                [(issueType): discontinuationsPerType]
            }
    }

    private List<String> getComponentDiscontinuations(Map oldData, Map newData) {
        def oldComponents = (oldData[JiraDataItem.TYPE_COMPONENTS] ?: [:]).keySet()
        def newComponents = (newData[JiraDataItem.TYPE_COMPONENTS] ?: [:]).keySet()
        (oldComponents - newComponents) as List
    }

    private Map addKeyAndVersionToComponentsWithout(Map jiraData) {
        def currentVersion = jiraData.version
        (jiraData[JiraDataItem.TYPE_COMPONENTS] ?: [:]).each { k, component ->
            jiraData[JiraDataItem.TYPE_COMPONENTS][k].key = k
            if (! component.versions) {
                jiraData[JiraDataItem.TYPE_COMPONENTS][k].versions = [currentVersion]
            }
        }
        jiraData
    }

    private static List getDiscontinuedLinks(Map savedData, List<String> discontinuations) {
        savedData.findAll { JiraDataItem.TYPES.contains(it.key) }.collect {
            issueType, Map issues ->
            def discontinuedLinks = issues.findAll { discontinuations.contains(it.key) }
                .collect { key, issue ->
                    def issueLinks = issue.findAll { JiraDataItem.TYPES.contains(it.key) }
                    issueLinks.collect { String linkType, List linkedIssues ->
                        linkedIssues.collect { targetKey ->
                            [origin: issue.key, target: targetKey, linkType: issueType, action: 'discontinue']
                        }
                    }.flatten()
                }.flatten()
            return discontinuedLinks
        }.flatten()
    }

    private static Map<String, List> buildChangesInLinks(Map oldData, Map updates) {
        def discontinuedLinks = getDiscontinuedLinks(oldData, (updates.discontinuations ?: []))
        def additionsAndChanges = getAdditionsAndChangesInLinks(updates)

        return (discontinuedLinks + additionsAndChanges).groupBy { it.target }
    }

    private static List getAdditionsAndChangesInLinks(Map newData) {
        def getLink = { String issueType, Map issue, String targetKey, Boolean isAnUpdate ->
            if (isAnUpdate) {
                issue.predecessors.collect {
                    [origin: issue.key, target: targetKey, linkType: issueType, action: 'change', replaces: it]
                }
            } else {
                [origin: issue.key, target: targetKey, linkType: issueType, action: 'add']
            }
        }

        newData.findAll { JiraDataItem.TYPES.contains(it.key) }.collect { issueType, issues ->
            issues.collect { String issueKey, Map issue ->
                def isAnUpdate = ! (issue.predecessors ?: []).isEmpty()

                def issueLinks = issue.findAll { JiraDataItem.TYPES.contains(it.key) }
                issueLinks.collect { String linkType, List linkedIssues ->
                    linkedIssues.collect { getLink(issueType, issue, it, isAnUpdate) }.flatten()
                }
            }
        }.flatten()
    }
    
    private static Map removeObsoleteIssues(Map jiraData, List<String> keysToRemove) {
        def result = jiraData.collectEntries { issueType, content ->
            if (JiraDataItem.TYPES.contains(issueType)) {
                [(issueType): content.findAll { ! keysToRemove.contains(it.key) } ]
            } else {
                [(issueType): content]
            }
        }
        return result
    }

    /**
     * Expected format is:
     *   issueType.issue."expandedPredecessors" -> [key:"", version:""]
     *   Note that an issue can only have a single successor in a single given version.
     *   An issue can only have multiple successors if they belong to different succeeding versions.
     * @param jiraData map of jira data
     * @return a Map with the issue keys as values and their respective predecessor keys as keys
     */
    private static Map getSuccessorIndex(Map jiraData) {
        def index = [:]
        jiraData.findAll { JiraDataItem.TYPES.contains(it.key) }.values().each { issueGroup ->
            issueGroup.values().each { issue ->
                (issue.expandedPredecessors ?: []).each { index[it.key] = issue.key }
            }
        }
        return index
    }

    /**
     * Recover the information about "preceding" issues for all the new ones that are an update on previously
     * released ones. That way we can provide all the changes in the documents
     * @param savedData data from old versions retrieved by the pipeline
     * @param newData data for the current version
     * @return Map new data with the issue predecessors expanded
     */
    
    private static Map expandPredecessorInformation(Map savedData, Map newData, List discontinuations) {
        def expandPredecessor = { String issueType, String issueKey, String predecessor ->
            def predecessorIssue = (savedData[issueType] ?: [:])[predecessor]
            if (!predecessorIssue) {
                throw new RuntimeException("Error: new issue '${issueKey}' references key '${predecessor}' " +
                    "of type '${issueType}' that cannot be found in the saved data for version '${savedData.version}'." +
                    "Existing issue list is '[${(savedData[issueType] ?: [:]).keySet().join(', ')}]'")
            }
            def existingPredecessors = (predecessorIssue.expandedPredecessors ?: [:])
            def result = [[key: predecessorIssue.key, versions: predecessorIssue.versions]]

            if (existingPredecessors) {
                result << existingPredecessors
            }
            result.flatten()
        }

        newData.collectEntries { issueType, content ->
            if (JiraDataItem.TYPES.contains(issueType)) {
                def updatedIssues = content.collectEntries { String issueKey, Map issue ->
                    def predecessors = issue.predecessors ?: []
                    if (predecessors.isEmpty()) {
                        [(issueKey): issue]
                    } else {
                        def expandedPredecessors = predecessors.collect { predecessor ->
                            expandPredecessor(issueType, issueKey, predecessor)
                        }.flatten()
                        // Get old links from predecessor (just one allowed)
                        def predecessorIssue = savedData.get(issueType).get(predecessors.first())
                        def updatedIssue = mergeJiraItemLinks(predecessorIssue, issue, discontinuations)

                        [(issueKey): (updatedIssue + [expandedPredecessors: expandedPredecessors])]
                    }
                }
                [(issueType): updatedIssues]
            } else {
                [(issueType): content]
            }
        }
    }

    private thumbnailImageReplacement(content) {
        def matcher = content =~ /<a.*id="(.*)_thumb".*href="(.*?)"/
        matcher.each {
            def imageMatcher = content =~ /<a.*id="${it[1]}_thumb".*src="(.*?)"/
            content = content.replace(imageMatcher[0][1], it[2])
        }
    }

}
