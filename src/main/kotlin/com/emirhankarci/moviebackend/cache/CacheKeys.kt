package com.emirhankarci.moviebackend.cache

import java.time.Duration

/**
 * Cache key patterns and TTL constants for Redis caching.
 * Provides consistent key naming and TTL management across the application.
 */
object CacheKeys {

    // ==================== TTL Constants ====================
    
    object TTL {
        /** 24 hours - for rarely changing data (movie details, actor info) */
        val LONG: Duration = Duration.ofHours(24)
        
        /** 6 hours - for moderately changing data (recommendations) */
        val MEDIUM: Duration = Duration.ofHours(6)
        
        /** 1 hour - for frequently changing data (popular movies, search results) */
        val SHORT: Duration = Duration.ofHours(1)
        
        /** 30 minutes - for very dynamic data */
        val VERY_SHORT: Duration = Duration.ofMinutes(30)
    }

    // ==================== Movie Keys ====================
    
    object Movie {
        private const val PREFIX = "movie"
        
        /** movie:{movieId} - Full movie details */
        fun detail(movieId: Long): String = "$PREFIX:$movieId"
        
        /** movie:{movieId}:credits - Cast and crew */
        fun credits(movieId: Long): String = "$PREFIX:$movieId:credits"
        
        /** movie:{movieId}:recommendations - Similar movies */
        fun recommendations(movieId: Long): String = "$PREFIX:$movieId:recommendations"
        
        /** movies:popular:page:{page} - Popular movies list */
        fun popular(page: Int = 1): String = "${PREFIX}s:popular:page:$page"
        
        /** Pattern for all movie keys */
        const val PATTERN_ALL = "$PREFIX:*"
    }

    // ==================== Actor Keys ====================
    
    object Actor {
        private const val PREFIX = "actor"
        
        /** actor:{actorId} - Actor details */
        fun detail(actorId: Long): String = "$PREFIX:$actorId"
        
        /** actor:{actorId}:filmography - Actor's movies */
        fun filmography(actorId: Long): String = "$PREFIX:$actorId:filmography"
        
        /** Pattern for all actor keys */
        const val PATTERN_ALL = "$PREFIX:*"
    }

    // ==================== Search Keys ====================
    
    object Search {
        private const val PREFIX = "search"
        
        /** search:{query}:page:{page} - Search results */
        fun results(query: String, page: Int = 1): String {
            val normalizedQuery = query.lowercase().trim().replace(" ", "_")
            return "$PREFIX:$normalizedQuery:page:$page"
        }
        
        /** discover:{filters_hash}:page:{page} - Discover results */
        fun discover(filtersHash: String, page: Int = 1): String = "discover:$filtersHash:page:$page"
        
        /** Pattern for all search keys */
        const val PATTERN_ALL = "$PREFIX:*"
    }

    // ==================== Collection Keys ====================
    
    object Collection {
        private const val PREFIX = "collection"
        
        /** collection:{collectionId} - TMDB collection details */
        fun detail(collectionId: Long): String = "$PREFIX:$collectionId"
        
        /** Pattern for all collection keys */
        const val PATTERN_ALL = "$PREFIX:*"
    }

    // ==================== Featured Keys ====================
    
    object Featured {
        private const val PREFIX = "featured"
        
        /** featured:movies - Featured movies list */
        const val MOVIES = "$PREFIX:movies"
        
        /** featured:{timeWindow} - Trending movies by time window (day/week) */
        fun trending(timeWindow: String): String = "$PREFIX:$timeWindow"
        
        /** Pattern for all featured keys */
        const val PATTERN_ALL = "$PREFIX:*"
    }

    // ==================== Chat Keys ====================
    
    object Chat {
        private const val PREFIX = "chat"
        
        /** chat:search:{query} - Chat movie search results */
        fun search(query: String): String {
            val normalized = query.lowercase().trim().replace(" ", "_")
            return "$PREFIX:search:$normalized"
        }
        
        /** Pattern for all chat keys */
        const val PATTERN_ALL = "$PREFIX:*"
    }
}
