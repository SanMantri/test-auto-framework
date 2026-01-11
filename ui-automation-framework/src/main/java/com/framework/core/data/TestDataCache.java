package com.framework.core.data;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TestDataCache - Thread-isolated test data storage
 *
 * Each test thread gets its own instance via ThreadLocal in BaseTest.
 * Data is automatically cleared after each test method.
 *
 * Use cases:
 * - Store IDs created during test setup (orderId, cartId, userId)
 * - Pass data between test steps
 * - Store expected values for assertions
 */
@Slf4j
public class TestDataCache {

    private final Map<String, Object> cache = new HashMap<>();

    // ═══════════════════════════════════════════════════════════════════════════
    // CORE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    public void put(String key, Object value) {
        cache.put(key, value);
        log.debug("TestDataCache: Stored [{}] = {}", key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) cache.getOrDefault(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T getRequired(String key) {
        T value = (T) cache.get(key);
        if (value == null) {
            throw new IllegalStateException("Required test data not found: " + key);
        }
        return value;
    }

    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
        log.debug("TestDataCache: Cleared all data");
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

    public Double getDouble(String key) {
        return get(key);
    }

    public BigDecimal getBigDecimal(String key) {
        return get(key);
    }

    public Boolean getBoolean(String key) {
        return get(key);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return get(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        return get(key);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(String key) {
        return get(key);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHAINING SUPPORT
    // ═══════════════════════════════════════════════════════════════════════════

    public TestDataCache set(String key, Object value) {
        put(key, value);
        return this;
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
        return new HashMap<>(cache);
    }

    public void putAll(Map<String, Object> data) {
        cache.putAll(data);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════════════════

    public void logContents() {
        log.info("TestDataCache contents ({} items):", cache.size());
        cache.forEach((k, v) -> log.info("  [{}] = {}", k, v));
    }

    @Override
    public String toString() {
        return "TestDataCache{" + cache + "}";
    }
}
