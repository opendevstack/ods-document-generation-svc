package org.ods.doc.gen.leva.doc.services



class DocumentHistoryEntry implements Map, Serializable {

    private final Long entryId
    private final Map delegate
    private final String projectVersion
    private final String previousProjectVersion
    String rational

    DocumentHistoryEntry(Map map, Long entryId, String projectVersion,
                         String previousProjectVersion, String rational) {
        def delegate = (Map) map.clone()
        delegate.keySet().removeAll(['entryId', 'projectVersion', 'previousProjectVersion', 'rational'])
        this.delegate = delegate
        this.entryId = entryId
        this.projectVersion = projectVersion
        this.previousProjectVersion = previousProjectVersion
        this.rational = rational
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
        return (delegate + [
            id: entryId,
            projectVersion: projectVersion,
            previousProjectVersion: previousProjectVersion,
            rational: rational,
        ]).keySet()
    }

    
    @Override
    Collection values() {
        return (delegate + [
            entryId: entryId,
            projectVersion: projectVersion,
            previousProjectVersion: previousProjectVersion,
            rational: rational,
        ]).values()
    }

    
    @Override
    Set<Entry> entrySet() {
        return (delegate + [
            entryId: entryId,
            projectVersion: projectVersion,
            previousProjectVersion: previousProjectVersion,
            rational: rational,
        ]).entrySet()
    }

    
    Long getEntryId() {
        return entryId
    }

    
    String getRational() {
        return rational
    }

    
    String getProjectVersion() {
        return projectVersion
    }

    
    String getPreviousProjectVersion() {
        return previousProjectVersion
    }

    
    Map getDelegate() {
        return delegate
    }

    
    DocumentHistoryEntry cloneIt() {
        def bos = new ByteArrayOutputStream()
        def os = new ObjectOutputStream(bos)
        os.writeObject(this.delegate)
        def ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))

        def newDelegate = ois.readObject()
        DocumentHistoryEntry result = new DocumentHistoryEntry(newDelegate,
            entryId, projectVersion, previousProjectVersion)
        result.rational = this.rational
        return result
    }

}
