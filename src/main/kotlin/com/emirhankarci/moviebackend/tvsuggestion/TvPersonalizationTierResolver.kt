package com.emirhankarci.moviebackend.tvsuggestion

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Resolves the personalization tier for TV series suggestions based on available user data.
 */
@Component
class TvPersonalizationTierResolver {

    companion object {
        private val logger = LoggerFactory.getLogger(TvPersonalizationTierResolver::class.java)
        private const val MIN_WATCHED_FOR_FULL = 3
        private const val MIN_WATCHLIST_FOR_FULL = 3
    }

    /**
     * Determines the personalization tier based on available data.
     *
     * Tier logic:
     * - FULL: Has TV preferences AND (watched >= 3 OR watchlist >= 3)
     * - PREFERENCES_BASED: Has TV preferences only
     * - WATCHLIST_BASED: Has watchlist only (no preferences)
     * - DIVERSE_POPULAR: No data at all
     */
    fun resolveTier(
        hasPreferences: Boolean,
        watchedCount: Int,
        watchlistCount: Int
    ): TvPersonalizationTier {
        val hasSignificantHistory = watchedCount >= MIN_WATCHED_FOR_FULL || 
                                    watchlistCount >= MIN_WATCHLIST_FOR_FULL

        val tier = when {
            hasPreferences && hasSignificantHistory -> TvPersonalizationTier.FULL
            hasPreferences -> TvPersonalizationTier.PREFERENCES_BASED
            watchlistCount > 0 -> TvPersonalizationTier.WATCHLIST_BASED
            else -> TvPersonalizationTier.DIVERSE_POPULAR
        }

        logger.debug(
            "Resolved tier: {} (prefs={}, watched={}, watchlist={})",
            tier, hasPreferences, watchedCount, watchlistCount
        )

        return tier
    }
}
