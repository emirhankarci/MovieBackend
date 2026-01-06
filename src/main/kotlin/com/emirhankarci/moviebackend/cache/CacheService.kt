package com.emirhankarci.moviebackend.cache

import java.time.Duration

/**
 * Generic cache service interface for caching operations.
 * Provides abstraction over the underlying cache implementation (Redis).
 */
interface CacheService {
    
    /**
     * Get a cached value by key.
     * @param key The cache key
     * @param type The expected type of the cached value
     * @return The cached value or null if not found
     */
    fun <T> get(key: String, type: Class<T>): T?
    
    /**
     * Set a value in cache with custom TTL.
     * @param key The cache key
     * @param value The value to cache
     * @param ttl Time to live duration
     */
    fun <T> set(key: String, value: T, ttl: Duration)
    
    /**
     * Set a value in cache with default TTL.
     * @param key The cache key
     * @param value The value to cache
     */
    fun <T> set(key: String, value: T)
    
    /**
     * Delete a cached value by key.
     * @param key The cache key
     * @return true if the key was deleted, false otherwise
     */
    fun delete(key: String): Boolean
    
    /**
     * Check if a key exists in cache.
     * @param key The cache key
     * @return true if the key exists, false otherwise
     */
    fun exists(key: String): Boolean
    
    /**
     * Delete all keys matching a pattern.
     * @param pattern The key pattern (e.g., "movie:*")
     * @return Number of keys deleted
     */
    fun deleteByPattern(pattern: String): Long
}
