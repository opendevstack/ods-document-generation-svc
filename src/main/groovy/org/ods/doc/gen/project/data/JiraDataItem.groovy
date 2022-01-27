package org.ods.doc.gen.project.data

class JiraDataItem implements Map, Serializable {
    static final String TYPE_BUGS = 'bugs'
    static final String TYPE_COMPONENTS = 'components'
    static final String TYPE_EPICS = 'epics'
    static final String TYPE_MITIGATIONS = 'mitigations'
    static final String TYPE_REQUIREMENTS = 'requirements'
    static final String TYPE_RISKS = 'risks'
    static final String TYPE_TECHSPECS = 'techSpecs'
    static final String TYPE_TESTS = 'tests'
    static final String TYPE_DOCS = 'docs'
    static final String TYPE_DOCTRACKING = 'docTrackings'

    static final List TYPES = [
    TYPE_BUGS,
    TYPE_COMPONENTS,
    TYPE_EPICS,
    TYPE_MITIGATIONS,
    TYPE_REQUIREMENTS,
    TYPE_RISKS,
    TYPE_TECHSPECS,
    TYPE_TESTS,
    TYPE_DOCS,
            ]

    static final List TYPES_WITH_STATUS = [
    TYPE_BUGS,
    TYPE_EPICS,
    TYPE_MITIGATIONS,
    TYPE_REQUIREMENTS,
    TYPE_RISKS,
    TYPE_TECHSPECS,
    TYPE_TESTS,
    TYPE_DOCS,
            ]

    static final List TYPES_TO_BE_CLOSED = [
    TYPE_EPICS,
    TYPE_MITIGATIONS,
    TYPE_REQUIREMENTS,
    TYPE_RISKS,
    TYPE_TECHSPECS,
    TYPE_TESTS,
    TYPE_DOCS,
            ]

    static final String ISSUE_STATUS_DONE = 'done'
    static final String ISSUE_STATUS_CANCELLED = 'cancelled'

    static final String ISSUE_TEST_EXECUTION_TYPE_AUTOMATED = 'automated'

    private final ProjectData project;
    private final String type
    private HashMap delegate

    JiraDataItem(ProjectData project, Map map, String type) {
        this.project = project;
        this.delegate = new HashMap(map)
        this.type = type
    }


    @Override
    int size() {
        return delegate.size()
    }


    @Override
    boolean isEmpty() {
        return delegate.isEmpty()
    }


    @Override
    boolean containsKey(Object key) {
        return delegate.containsKey(key)
    }


    @Override
    boolean containsValue(Object value) {
        return delegate.containsValue(value)
    }


    @Override
    Object get(Object key) {
        return delegate.get(key)
    }


    @Override
    Object put(Object key, Object value) {
        return delegate.put(key, value)
    }


    @Override
    Object remove(Object key) {
        return delegate.remove(key)
    }


    @Override
    void putAll(Map m) {
        delegate.putAll(m)
    }


    @Override
    void clear() {
        delegate.clear()
    }


    @Override
    Set keySet() {
        return delegate.keySet()
    }


    @Override
    Collection values() {
        return delegate.values()
    }


    @Override
    Set<Entry> entrySet() {
        return delegate.entrySet()
    }


    String getType() {
        return type
    }


    Map getDelegate() {
        return delegate
    }


    JiraDataItem cloneIt() {
        def bos = new ByteArrayOutputStream()
        def os = new ObjectOutputStream(bos)
        os.writeObject(this.delegate)
        def ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))

        def newDelegate = ois.readObject()
        JiraDataItem result = new JiraDataItem(newDelegate, type)
        return result
    }

    // FIXME: why can we not invoke derived methods in short form, e.g. .resolvedBugs?
    // Reason: that is because when you do this.resolvedBugs it goes to the get method for delegate dictionary
    // and does deletage.resolvedBugs  And we have no entry there
    // An option would be to put some logic for this in the get() method of this class

    private List<JiraDataItem> getResolvedReferences(String type) {
        // Reference this within jiraResolved (contains readily resolved references to other entities)
        def item = project.data.jiraResolved[this.type][this.key]
        return item && item[type] ? item[type] : []
    }


    List<JiraDataItem> getResolvedBugs() {
        return this.getResolvedReferences(TYPE_BUGS)
    }


    List<JiraDataItem> getResolvedComponents() {
        return this.getResolvedReferences(TYPE_COMPONENTS)
    }


    List<JiraDataItem> getResolvedEpics() {
        return this.getResolvedReferences(TYPE_EPICS)
    }


    List<JiraDataItem> getResolvedMitigations() {
        return this.getResolvedReferences(TYPE_MITIGATIONS)
    }


    List<JiraDataItem> getResolvedSystemRequirements() {
        return this.getResolvedReferences(TYPE_REQUIREMENTS)
    }


    List<JiraDataItem> getResolvedRisks() {
        return this.getResolvedReferences(TYPE_RISKS)
    }


    List<JiraDataItem> getResolvedTechnicalSpecifications() {
        return this.getResolvedReferences(TYPE_TECHSPECS)
    }


    List<JiraDataItem> getResolvedTests() {
        return this.getResolvedReferences(TYPE_TESTS)
    }

}
