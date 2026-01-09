package com.emirhankarci.moviebackend.tvsuggestion

import com.emirhankarci.moviebackend.cache.CacheService
import com.emirhankarci.moviebackend.featured.FeaturedTvSeriesResult
import com.emirhankarci.moviebackend.featured.FeaturedTvSeriesService
import com.emirhankarci.moviebackend.featured.TimeWindow
import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate

@Service
class PersonalizedFeaturedTvService(
    private val featuredTvSeriesService: FeaturedTvSeriesService,
    private val tvUserProfileAggregator: TvUserProfileAggregator,
    private val tvHookMessageGenerator: TvHookMessageGenerator,
    private val cacheService: CacheService,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PersonalizedFeaturedTvService::class.java)
        private const val FEATURED_TV_COUNT = 10
        private val CACHE_TTL: Duration = Duration.ofHours(24)
    }

    /**
     * Get personalized featured TV series for a user
     */
    fun getPersonalizedFeaturedTvSeries(username: String): PersonalizedFeaturedTvResult {
        val user = userRepository.findByUsername(username)
            ?: return PersonalizedFeaturedTvResult.Error("USER_NOT_FOUND", "Kullanıcı bulunamadı")

        val userId = user.id!!
        val today = LocalDate.now()
        val cacheKey = "featured:tv:personalized:$userId:$today"

        // Check cache first
        cacheService.get(cacheKey, PersonalizedFeaturedTvCacheData::class.java)?.let { cachedData ->
            logger.info("Cache HIT for personalized featured TV series: user={}", username)
            return PersonalizedFeaturedTvResult.Success(
                PersonalizedFeaturedTvResponse(
                    tvSeries = cachedData.tvSeries,
                    cached = true,
                    generatedAt = cachedData.generatedAt,
                    metadata = cachedData.metadata
                )
            )
        }

        logger.info("Cache MISS for personalized featured TV series: user={}, generating...", username)

        // Get trending TV series
        val trendingResult = featuredTvSeriesService.getFeaturedTvSeries(TimeWindow.DAY, FEATURED_TV_COUNT)
        val featuredTvSeries = when (trendingResult) {
            is FeaturedTvSeriesResult.Success -> trendingResult.tvSeries
            is FeaturedTvSeriesResult.Error -> {
                logger.error("Failed to fetch trending TV series: {}", trendingResult.message)
                return PersonalizedFeaturedTvResult.Error("TRENDING_ERROR", "Trending diziler alınamadı")
            }
        }

        // Get user TV profile (may fail, that's okay)
        val userProfile = try {
            tvUserProfileAggregator.aggregateTvUserProfile(userId)
        } catch (e: Exception) {
            logger.warn("Failed to load user TV profile for {}: {}", username, e.message)
            null
        }

        // Generate hook messages
        val hookMessages = tvHookMessageGenerator.generateHookMessages(featuredTvSeries, userProfile)
        val hookMessageMap = hookMessages.associateBy { it.seriesId }

        // Combine TV series with hook messages
        val personalizedTvSeries = featuredTvSeries.map { series ->
            PersonalizedFeaturedTvSeries(
                id = series.id,
                name = series.name,
                backdropPath = series.backdropPath,
                tagline = series.tagline,
                rating = series.rating,
                firstAirYear = series.firstAirYear,
                genres = series.genres,
                hookMessage = hookMessageMap[series.id]?.message ?: ""
            )
        }

        // Build metadata
        val metadata = buildMetadata(userProfile)

        // Cache the results
        val cacheData = PersonalizedFeaturedTvCacheData(
            tvSeries = personalizedTvSeries,
            metadata = metadata,
            generatedAt = today.toString()
        )

        try {
            cacheService.set(cacheKey, cacheData, CACHE_TTL)
            logger.info("Cached personalized featured TV series for user={}", username)
        } catch (e: Exception) {
            logger.warn("Failed to cache personalized featured TV series: {}", e.message)
        }

        return PersonalizedFeaturedTvResult.Success(
            PersonalizedFeaturedTvResponse(
                tvSeries = personalizedTvSeries,
                cached = false,
                generatedAt = today.toString(),
                metadata = metadata
            )
        )
    }

    private fun buildMetadata(userProfile: TvUserProfile?): TvPersonalizationMetadata {
        if (userProfile == null) {
            return TvPersonalizationMetadata(
                personalizationTier = "NONE",
                dataSources = emptyList(),
                profileSummary = null
            )
        }

        val dataSources = mutableListOf<String>()
        if (userProfile.watchedTvSeries.isNotEmpty()) dataSources.add("watched_tv_series")
        if (userProfile.tvWatchlist.isNotEmpty()) dataSources.add("tv_watchlist")
        if (userProfile.preferences != null) dataSources.add("preferences")
        if (userProfile.likedTvSeriesIds.isNotEmpty() || userProfile.dislikedTvSeriesIds.isNotEmpty()) {
            dataSources.add("reactions")
        }

        val profileSummary = TvProfileSummary(
            watchedCount = userProfile.watchedTvSeries.size,
            watchlistCount = userProfile.tvWatchlist.size,
            hasPreferences = userProfile.preferences != null,
            topGenres = userProfile.preferences?.genres?.take(3)
        )

        return TvPersonalizationMetadata(
            personalizationTier = userProfile.personalizationTier.name,
            dataSources = dataSources,
            profileSummary = profileSummary
        )
    }
}
