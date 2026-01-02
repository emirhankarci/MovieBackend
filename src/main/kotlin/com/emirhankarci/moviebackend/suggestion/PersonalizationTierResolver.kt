package com.emirhankarci.moviebackend.suggestion

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Resolves which personalization tier to use based on available user data.
 * 
 * Tier Decision Logic:
 * - FULL: preferences exist AND (watchedCount >= 3 OR watchlistCount >= 3)
 * - PREFERENCES_BASED: preferences exist AND watchedCount < 3 AND watchlistCount < 3
 * - WATCHLIST_BASED: no preferences AND watchlistCount >= 1
 * - DIVERSE_POPULAR: no data at all
 */
@Component
class PersonalizationTierResolver {
    
    companion object {
        private val logger = LoggerFactory.getLogger(PersonalizationTierResolver::class.java)
        private const val MIN_DATA_FOR_FULL = 3
    }
    
    fun resolveTier(
        hasPreferences: Boolean,
        watchedCount: Int,
        watchlistCount: Int
    ): PersonalizationTier {
        val tier = when {
            hasPreferences && (watchedCount >= MIN_DATA_FOR_FULL || watchlistCount >= MIN_DATA_FOR_FULL) -> {
                PersonalizationTier.FULL
            }
            hasPreferences -> {
                PersonalizationTier.PREFERENCES_BASED
            }
            watchlistCount >= 1 -> {
                PersonalizationTier.WATCHLIST_BASED
            }
            else -> {
                PersonalizationTier.DIVERSE_POPULAR
            }
        }
        
        logger.debug(
            "Resolved tier: {} (hasPreferences={}, watchedCount={}, watchlistCount={})",
            tier, hasPreferences, watchedCount, watchlistCount
        )
        
        return tier
    }
}
