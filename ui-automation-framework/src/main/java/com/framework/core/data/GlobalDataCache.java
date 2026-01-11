package com.framework.core.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GlobalDataCache - Suite-level shared data storage
 *
 * Thread-safe storage for data shared across all tests.
 * Initialized in @BeforeSuite, available throughout test run.
 *
 * Typical contents:
 * - Configuration values
 * - Authentication tokens/state
 * - Master/reference data
 * - Cached API responses
 */
@Slf4j
@Component
public class GlobalDataCache {

    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════════════════════
    // CORE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    public void put(String key, Object value) {
        cache.put(key, value);
        log.debug("GlobalDataCache: Stored [{}] = {}", key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) cache.getOrDefault(key, defaultValue);
    }

    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }

    public void remove(String key) {
        cache.remove(key);
        log.debug("GlobalDataCache: Removed [{}]", key);
    }

    public void clear() {
        cache.clear();
        log.info("GlobalDataCache: Cleared all data");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TYPED GETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public String getString(String key) {
        return get(key);
    }

    public String getString(String key, String defaultValue) {
        return get(key, defaultValue);
    }

    public Integer getInteger(String key) {
        return get(key);
    }

    public Integer getInteger(String key, Integer defaultValue) {
        return get(key, defaultValue);
    }

    public Long getLong(String key) {
        return get(key);
    }

    public Boolean getBoolean(String key) {
        return get(key);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return get(key, defaultValue);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════════

    public Set<String> keys() {
        return cache.keySet();
    }

    public int size() {
        return cache.size();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    public Map<String, Object> getAll() {
        return new ConcurrentHashMap<>(cache);
    }

    public void putAll(Map<String, Object> data) {
        cache.putAll(data);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════════════════

    public void logContents() {
        log.info("GlobalDataCache contents ({} items):", cache.size());
        cache.forEach((k, v) -> log.info("  [{}] = {}", k, v));
    }
}
