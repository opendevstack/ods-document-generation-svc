package org.ods.shared.lib.services

@Singleton
class ServiceRegistry {

    private final registry = [:]

    void add(Class<?> type, def service) {
        registry[type.name] = service
    }

    def <T> T get(Class<T> type) {
        return registry[type.name] as T
    }

    def clear() {
        registry.clear()
    }

}
