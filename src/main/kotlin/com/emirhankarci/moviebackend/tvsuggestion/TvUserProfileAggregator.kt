package com.emirhankarci.moviebackend.tvsuggestion

import com.emirhankarci.moviebackend.chat.ChatReactionRepository
import com.emirhankarci.moviebackend.chat.ReactionType
import com.emirhankarci.moviebackend.preferences.UserPreferencesRepository
import com.emirhankarci.moviebackend.tvwatchlist.TvWatchlistRepository
import com.emirhankarci.moviebackend.watchedepisode.WatchedEpisodeRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Aggregates all TV series user data sources for personalized suggestions.
 */
@Service
class TvUserProfileAggregator(
    private val tvWatchlistRepository: TvWatchlistRepository,
    private val watchedEpisodeRepository: WatchedEpisodeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val chatReactionRepository: ChatReactionRepository,
    private val tierResolver: TvPersonalizationTierResolver
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvUserProfileAggregator::class.java)
        private const val MAX_WATCHED_SERIES = 50
    }

    fun aggregateTvUserProfile(userId: Long): TvUserProfile {
        logger.info("Aggregating TV profile for user {}", userId)

        // Collect watched TV series from watched episodes
        val watchedTvSeries = collectWatchedTvSeries(userId)
        logger.debug("Found {} watched TV series for user {}", watchedTvSeries.size, userId)

        // Collect TV watchlist
        val tvWatchlist = tvWatchlistRepository.findByUserIdOrderByCreatedAtDesc(userId).map { item ->
            TvWatchlistInfo(
                seriesId = item.seriesId,
                seriesName = item.seriesName
            )
        }
        logger.debug("Found {} TV watchlist items for user {}", tvWatchlist.size, userId)

        // Collect TV preferences (may not exist)
        val preferencesOpt = userPreferencesRepository.findByUserId(userId)
        val preferences = if (preferencesOpt.isPresent) {
            val prefs = preferencesOpt.get()
            val tvSeriesIds = prefs.favoriteTvSeriesIds?.split(",")
                ?.mapNotNull { it.trim().toLongOrNull() }
                ?: emptyList()
            
            if (tvSeriesIds.isNotEmpty()) {
                TvPreferencesInfo(
                    genres = prefs.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    preferredEra = prefs.preferredEra,
                    moods = prefs.moods.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    favoriteTvSeriesIds = tvSeriesIds
                )
            } else {
                null
            }
        } else {
            null
        }
        logger.debug("User {} has TV preferences: {}", userId, preferences != null)

        // Collect TV series reactions (likes/dislikes)
        // Note: ChatReaction currently stores movieId, we'll filter for TV series reactions
        // For now, we'll use an empty list as TV series reactions need separate handling
        val likedTvSeriesIds = emptyList<Int>()
        val dislikedTvSeriesIds = emptyList<Int>()
        logger.debug("Found {} liked and {} disliked TV series for user {}",
            likedTvSeriesIds.size, dislikedTvSeriesIds.size, userId)

        // Build exclusion list
        val exclusionList = buildExclusionList(watchedTvSeries, tvWatchlist, dislikedTvSeriesIds)

        // Resolve personalization tier
        val tier = tierResolver.resolveTier(
            hasPreferences = preferences != null,
            watchedCount = watchedTvSeries.size,
            watchlistCount = tvWatchlist.size
        )
        logger.info("User {} TV profile aggregated with tier: {}", userId, tier)

        return TvUserProfile(
            userId = userId,
            watchedTvSeries = watchedTvSeries,
            tvWatchlist = tvWatchlist,
            preferences = preferences,
            likedTvSeriesIds = likedTvSeriesIds,
            dislikedTvSeriesIds = dislikedTvSeriesIds,
            personalizationTier = tier,
            exclusionList = exclusionList
        )
    }

    private fun collectWatchedTvSeries(userId: Long): List<WatchedTvSeriesInfo> {
        // Get watched series summaries
        val pageable = PageRequest.of(0, MAX_WATCHED_SERIES)
        val watchedSeries = watchedEpisodeRepository.findWatchedSeriesByUserIdOrderByLastWatchedAtDesc(userId, pageable)

        return watchedSeries.content.map { summary ->
            val episodeCount = watchedEpisodeRepository.countByUserIdAndSeriesId(userId, summary.seriesId)
            WatchedTvSeriesInfo(
                seriesId = summary.seriesId,
                seriesName = summary.seriesName,
                episodeCount = episodeCount
            )
        }
    }

    private fun buildExclusionList(
        watchedTvSeries: List<WatchedTvSeriesInfo>,
        tvWatchlist: List<TvWatchlistInfo>,
        dislikedTvSeriesIds: List<Int>
    ): TvExclusionList {
        val watchedIds = watchedTvSeries.map { it.seriesId }.toSet()
        val watchlistIds = tvWatchlist.map { it.seriesId }.toSet()

        val allTitles = mutableSetOf<String>()
        allTitles.addAll(watchedTvSeries.map { it.seriesName })
        allTitles.addAll(tvWatchlist.map { it.seriesName })

        return TvExclusionList(
            watchedTvSeriesIds = watchedIds,
            tvWatchlistIds = watchlistIds,
            dislikedTvSeriesIds = dislikedTvSeriesIds.toSet(),
            allExcludedTitles = allTitles
        )
    }
}
