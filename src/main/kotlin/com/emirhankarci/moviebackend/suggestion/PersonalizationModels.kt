package com.emirhankarci.moviebackend.suggestion

import java.math.BigDecimal

/**
 * Personalization tier determines which prompt strategy to use based on available user data.
 */
enum class PersonalizationTier {
    FULL,              // Has preferences + watched (>=3) or watchlist (>=3)
    PREFERENCES_BASED, // Has preferences only (new user after onboarding)
    WATCHLIST_BASED,   // Has watchlist only (no preferences)
    DIVERSE_POPULAR    // No data at all
}

/**
 * Aggregated user profile containing all data sources for personalization.
 */
data class UserProfile(
    val userId: Long,
    val watchedMovies: List<WatchedMovieInfo>,
    val watchlistMovies: List<WatchlistMovieInfo>,
    val preferences: UserPreferencesInfo?,
    val likedMovieIds: List<Int>,
    val dislikedMovieIds: List<Int>,
    val personalizationTier: PersonalizationTier,
    val exclusionList: ExclusionList
)

/**
 * Watched movie information with user rating.
 */
data class WatchedMovieInfo(
    val movieId: Long,
    val title: String,
    val userRating: BigDecimal?
)

/**
 * Watchlist movie information.
 */
data class WatchlistMovieInfo(
    val movieId: Long,
    val title: String
)

/**
 * User preferences extracted from UserPreferences entity.
 */
data class UserPreferencesInfo(
    val genres: List<String>,
    val preferredEra: String,
    val moods: List<String>,
    val favoriteMovieIds: List<Long>
)

/**
 * Complete exclusion list containing all movies that should not be recommended.
 */
data class ExclusionList(
    val watchedMovieIds: Set<Long>,
    val watchlistMovieIds: Set<Long>,
    val dislikedMovieIds: Set<Int>,
    val allExcludedTitles: Set<String>
) {
    fun containsMovieId(movieId: Long): Boolean {
        return watchedMovieIds.contains(movieId) || 
               watchlistMovieIds.contains(movieId) ||
               dislikedMovieIds.contains(movieId.toInt())
    }
    
    fun containsTitle(title: String): Boolean {
        return allExcludedTitles.any { it.equals(title, ignoreCase = true) }
    }
    
    companion object {
        fun empty() = ExclusionList(
            watchedMovieIds = emptySet(),
            watchlistMovieIds = emptySet(),
            dislikedMovieIds = emptySet(),
            allExcludedTitles = emptySet()
        )
    }
}
