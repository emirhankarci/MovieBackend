package com.emirhankarci.moviebackend.suggestion

import com.emirhankarci.moviebackend.chat.ChatReactionRepository
import com.emirhankarci.moviebackend.chat.ReactionType
import com.emirhankarci.moviebackend.movie.WatchlistRepository
import com.emirhankarci.moviebackend.preferences.UserPreferencesRepository
import com.emirhankarci.moviebackend.watched.WatchedMovieRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Aggregates all user data sources for personalized suggestions.
 */
@Service
class UserProfileAggregator(
    private val watchedMovieRepository: WatchedMovieRepository,
    private val watchlistRepository: WatchlistRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val chatReactionRepository: ChatReactionRepository,
    private val tierResolver: PersonalizationTierResolver
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UserProfileAggregator::class.java)
    }

    fun aggregateUserProfile(userId: Long): UserProfile {
        logger.info("Aggregating profile for user {}", userId)
        
        // Collect watched movies
        val watchedMovies = watchedMovieRepository.findByUserId(userId).map { movie ->
            WatchedMovieInfo(
                movieId = movie.movieId,
                title = movie.movieTitle,
                userRating = movie.userRating
            )
        }
        logger.debug("Found {} watched movies for user {}", watchedMovies.size, userId)
        
        // Collect watchlist movies
        val watchlistMovies = watchlistRepository.findByUserId(userId).map { item ->
            WatchlistMovieInfo(
                movieId = item.movieId,
                title = item.movieTitle
            )
        }
        logger.debug("Found {} watchlist movies for user {}", watchlistMovies.size, userId)
        
        // Collect user preferences (may not exist)
        val preferencesOpt = userPreferencesRepository.findByUserId(userId)
        val preferences = if (preferencesOpt.isPresent) {
            val prefs = preferencesOpt.get()
            UserPreferencesInfo(
                genres = prefs.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                preferredEra = prefs.preferredEra,
                moods = prefs.moods.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                favoriteMovieIds = prefs.favoriteMovieIds.split(",")
                    .mapNotNull { it.trim().toLongOrNull() }
            )
        } else {
            null
        }
        logger.debug("User {} has preferences: {}", userId, preferences != null)
        
        // Collect chat reactions
        val reactions = chatReactionRepository.findByUserId(userId)
        val likedMovieIds = reactions
            .filter { it.reaction == ReactionType.LIKE && it.movieId != null }
            .mapNotNull { it.movieId }
        val dislikedMovieIds = reactions
            .filter { it.reaction == ReactionType.DISLIKE && it.movieId != null }
            .mapNotNull { it.movieId }
        logger.debug("Found {} liked and {} disliked movies for user {}", 
            likedMovieIds.size, dislikedMovieIds.size, userId)
        
        // Build exclusion list
        val exclusionList = buildExclusionList(watchedMovies, watchlistMovies, dislikedMovieIds)
        
        // Resolve personalization tier
        val tier = tierResolver.resolveTier(
            hasPreferences = preferences != null,
            watchedCount = watchedMovies.size,
            watchlistCount = watchlistMovies.size
        )
        logger.info("User {} profile aggregated with tier: {}", userId, tier)
        
        return UserProfile(
            userId = userId,
            watchedMovies = watchedMovies,
            watchlistMovies = watchlistMovies,
            preferences = preferences,
            likedMovieIds = likedMovieIds,
            dislikedMovieIds = dislikedMovieIds,
            personalizationTier = tier,
            exclusionList = exclusionList
        )
    }


    private fun buildExclusionList(
        watchedMovies: List<WatchedMovieInfo>,
        watchlistMovies: List<WatchlistMovieInfo>,
        dislikedMovieIds: List<Int>
    ): ExclusionList {
        val watchedIds = watchedMovies.map { it.movieId }.toSet()
        val watchlistIds = watchlistMovies.map { it.movieId }.toSet()
        
        val allTitles = mutableSetOf<String>()
        allTitles.addAll(watchedMovies.map { it.title })
        allTitles.addAll(watchlistMovies.map { it.title })
        
        return ExclusionList(
            watchedMovieIds = watchedIds,
            watchlistMovieIds = watchlistIds,
            dislikedMovieIds = dislikedMovieIds.toSet(),
            allExcludedTitles = allTitles
        )
    }
}
