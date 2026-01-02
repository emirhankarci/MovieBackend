package com.emirhankarci.moviebackend.featured

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache entry with expiration time
 */
data class CacheEntry<T>(
    val data: T,
    val expiresAt: Instant
)

/**
 * In-memory cache for featured movies with TTL support
 */
@Component
class FeaturedMoviesCache {
    
    private val cache = ConcurrentHashMap<String, CacheEntry<List<FeaturedMovie>>>()

    /**
     * Get cached movies if not expired
     */
    fun get(key: String): List<FeaturedMovie>? {
        val entry = cache[key] ?: return null
        return if (Instant.now().isBefore(entry.expiresAt)) {
            entry.data
        } else {
            cache.remove(key)
            null
        }
    }

    /**
     * Store movies in cache with TTL
     */
    fun put(key: String, movies: List<FeaturedMovie>, ttlHours: Long) {
        val expiresAt = Instant.now().plusSeconds(ttlHours * 3600)
        cache[key] = CacheEntry(movies, expiresAt)
    }

    /**
     * Build cache key from time window
     */
    fun buildKey(timeWindow: TimeWindow): String = "featured_${timeWindow.value}"

    /**
     * Clear all cache entries (for testing)
     */
    fun clear() {
        cache.clear()
    }
}
