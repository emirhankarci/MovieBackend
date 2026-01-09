package com.emirhankarci.moviebackend.tvsuggestion

import java.math.BigDecimal

/**
 * Personalization tier determines which prompt strategy to use based on available user data.
 * Reuses the same tiers as movie suggestions for consistency.
 */
enum class TvPersonalizationTier {
    FULL,              // Has preferences + watched (>=3) or watchlist (>=3)
    PREFERENCES_BASED, // Has preferences only (new user after onboarding)
    WATCHLIST_BASED,   // Has watchlist only (no preferences)
    DIVERSE_POPULAR    // No data at all
}

/**
 * Aggregated TV user profile containing all data sources for personalization.
 */
data class TvUserProfile(
    val userId: Long,
    val watchedTvSeries: List<WatchedTvSeriesInfo>,
    val tvWatchlist: List<TvWatchlistInfo>,
    val preferences: TvPreferencesInfo?,
    val likedTvSeriesIds: List<Int>,
    val dislikedTvSeriesIds: List<Int>,
    val personalizationTier: TvPersonalizationTier,
    val exclusionList: TvExclusionList
)

/**
 * Watched TV series information aggregated from watched episodes.
 */
data class WatchedTvSeriesInfo(
    val seriesId: Long,
    val seriesName: String,
    val episodeCount: Int,
    val averageRating: BigDecimal? = null
)

/**
 * TV watchlist item information.
 */
data class TvWatchlistInfo(
    val seriesId: Long,
    val seriesName: String
)

/**
 * TV preferences extracted from UserPreferences entity.
 */
data class TvPreferencesInfo(
    val genres: List<String>,
    val preferredEra: String,
    val moods: List<String>,
    val favoriteTvSeriesIds: List<Long>
)

/**
 * Complete exclusion list containing all TV series that should not be recommended.
 */
data class TvExclusionList(
    val watchedTvSeriesIds: Set<Long>,
    val tvWatchlistIds: Set<Long>,
    val dislikedTvSeriesIds: Set<Int>,
    val allExcludedTitles: Set<String>
) {
    fun containsSeriesId(seriesId: Long): Boolean {
        return watchedTvSeriesIds.contains(seriesId) ||
               tvWatchlistIds.contains(seriesId) ||
               dislikedTvSeriesIds.contains(seriesId.toInt())
    }

    fun containsTitle(title: String): Boolean {
        return allExcludedTitles.any { it.equals(title, ignoreCase = true) }
    }

    companion object {
        fun empty() = TvExclusionList(
            watchedTvSeriesIds = emptySet(),
            tvWatchlistIds = emptySet(),
            dislikedTvSeriesIds = emptySet(),
            allExcludedTitles = emptySet()
        )
    }
}
