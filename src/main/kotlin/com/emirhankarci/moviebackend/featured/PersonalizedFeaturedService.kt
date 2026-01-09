package com.emirhankarci.moviebackend.featured

import com.emirhankarci.moviebackend.cache.CacheKeys
import com.emirhankarci.moviebackend.cache.CacheService
import com.emirhankarci.moviebackend.suggestion.ProfileSummary
import com.emirhankarci.moviebackend.suggestion.UserProfile
import com.emirhankarci.moviebackend.suggestion.UserProfileAggregator
import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate

@Service
class PersonalizedFeaturedService(
    private val featuredMoviesService: FeaturedMoviesService,
    private val userProfileAggregator: UserProfileAggregator,
    private val hookMessageGenerator: HookMessageGenerator,
    private val cacheService: CacheService,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PersonalizedFeaturedService::class.java)
        private const val FEATURED_MOVIE_COUNT = 10
        private val CACHE_TTL: Duration = Duration.ofHours(24)
    }

    /**
     * Get personalized featured movies for a user
     */
    fun getPersonalizedFeaturedMovies(username: String): PersonalizedFeaturedResult {
        val user = userRepository.findByUsername(username)
            ?: return PersonalizedFeaturedResult.Error("USER_NOT_FOUND", "Kullanıcı bulunamadı")

        val userId = user.id!!
        val today = LocalDate.now()
        val cacheKey = CacheKeys.Featured.personalized(userId, today.toString())

        // Check cache first
        cacheService.get(cacheKey, PersonalizedFeaturedCacheData::class.java)?.let { cachedData ->
            logger.info("Cache HIT for personalized featured movies: user={}", username)
            return PersonalizedFeaturedResult.Success(
                PersonalizedFeaturedResponse(
                    movies = cachedData.movies,
                    cached = true,
                    generatedAt = cachedData.generatedAt,
                    metadata = cachedData.metadata
                )
            )
        }

        logger.info("Cache MISS for personalized featured movies: user={}, generating...", username)

        // Get trending movies
        val trendingResult = featuredMoviesService.getFeaturedMovies(TimeWindow.DAY, FEATURED_MOVIE_COUNT)
        val featuredMovies = when (trendingResult) {
            is FeaturedMoviesResult.Success -> trendingResult.movies
            is FeaturedMoviesResult.Error -> {
                logger.error("Failed to fetch trending movies: {}", trendingResult.message)
                return PersonalizedFeaturedResult.Error("TRENDING_ERROR", "Trending filmler alınamadı")
            }
        }

        // Get user profile (may fail, that's okay)
        val userProfile = try {
            userProfileAggregator.aggregateUserProfile(userId)
        } catch (e: Exception) {
            logger.warn("Failed to load user profile for {}: {}", username, e.message)
            null
        }

        // Generate hook messages
        val hookMessages = hookMessageGenerator.generateHookMessages(featuredMovies, userProfile)
        val hookMessageMap = hookMessages.associateBy { it.movieId }

        // Combine movies with hook messages
        val personalizedMovies = featuredMovies.map { movie ->
            PersonalizedFeaturedMovie(
                id = movie.id,
                title = movie.title,
                backdropPath = movie.backdropPath,
                tagline = movie.tagline,
                rating = movie.rating,
                releaseYear = movie.releaseYear,
                genres = movie.genres,
                hookMessage = hookMessageMap[movie.id]?.message ?: ""
            )
        }

        // Build metadata
        val metadata = buildMetadata(userProfile)

        // Cache the results
        val cacheData = PersonalizedFeaturedCacheData(
            movies = personalizedMovies,
            metadata = metadata,
            generatedAt = today.toString()
        )
        
        try {
            cacheService.set(cacheKey, cacheData, CACHE_TTL)
            logger.info("Cached personalized featured movies for user={}", username)
        } catch (e: Exception) {
            logger.warn("Failed to cache personalized featured movies: {}", e.message)
        }

        return PersonalizedFeaturedResult.Success(
            PersonalizedFeaturedResponse(
                movies = personalizedMovies,
                cached = false,
                generatedAt = today.toString(),
                metadata = metadata
            )
        )
    }

    private fun buildMetadata(userProfile: UserProfile?): PersonalizationMetadata {
        if (userProfile == null) {
            return PersonalizationMetadata(
                personalizationTier = "NONE",
                dataSources = emptyList(),
                profileSummary = null
            )
        }

        val dataSources = mutableListOf<String>()
        if (userProfile.watchedMovies.isNotEmpty()) dataSources.add("watched_movies")
        if (userProfile.watchlistMovies.isNotEmpty()) dataSources.add("watchlist")
        if (userProfile.preferences != null) dataSources.add("preferences")
        if (userProfile.likedMovieIds.isNotEmpty() || userProfile.dislikedMovieIds.isNotEmpty()) {
            dataSources.add("reactions")
        }

        val profileSummary = ProfileSummary(
            watchedCount = userProfile.watchedMovies.size,
            watchlistCount = userProfile.watchlistMovies.size,
            hasPreferences = userProfile.preferences != null,
            topGenres = userProfile.preferences?.genres?.take(3)
        )

        return PersonalizationMetadata(
            personalizationTier = userProfile.personalizationTier.name,
            dataSources = dataSources,
            profileSummary = profileSummary
        )
    }
}
