package com.emirhankarci.moviebackend.suggestion

import com.emirhankarci.moviebackend.chat.*
import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class RefreshSuggestionService(
    private val trackerRepository: SuggestionRefreshTrackerRepository,
    private val userRepository: UserRepository,
    private val aiService: AiService,
    private val tmdbService: TmdbService,
    private val promptBuilder: SuggestionPromptBuilder,
    private val responseParser: SuggestionResponseParser,
    private val userProfileAggregator: UserProfileAggregator
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RefreshSuggestionService::class.java)
        private const val REQUIRED_MOVIES = 4
        private const val MAX_RETRIES_PER_SLOT = 3
    }

    @Value("\${suggestion.refresh.default-limit:5}")
    private var defaultRefreshLimit: Int = 5

    fun getRefreshLimitForUser(userId: Long): Int {
        // Future: look up user tier and return tier-specific limit
        // For now, return default limit for all users
        return defaultRefreshLimit
    }

    fun getRefreshStatus(username: String): SuggestionResult<RefreshStatusResponse> {
        val user = userRepository.findByUsername(username)
            ?: return SuggestionResult.Error("User not found", SuggestionErrorCode.USER_NOT_FOUND)

        val userId = user.id!!
        val today = LocalDate.now()
        val limit = getRefreshLimitForUser(userId)
        val tracker = getOrCreateTracker(user, today)
        val remaining = (limit - tracker.refreshCount).coerceAtLeast(0)
        val resetsAt = LocalDateTime.of(today.plusDays(1), LocalTime.MIDNIGHT)

        return SuggestionResult.Success(
            RefreshStatusResponse(
                remainingRefreshes = remaining,
                dailyLimit = limit,
                resetsAt = resetsAt,
                usedToday = tracker.refreshCount
            )
        )
    }

    @Transactional
    fun refreshSuggestions(username: String): SuggestionResult<RefreshSuggestionsResponse> {
        val user = userRepository.findByUsername(username)
            ?: return SuggestionResult.Error("User not found", SuggestionErrorCode.USER_NOT_FOUND)

        val userId = user.id!!
        val today = LocalDate.now()
        val limit = getRefreshLimitForUser(userId)
        
        // Check refresh limit
        val tracker = getOrCreateTracker(user, today)
        if (tracker.refreshCount >= limit) {
            val resetsAt = LocalDateTime.of(today.plusDays(1), LocalTime.MIDNIGHT)
            return SuggestionResult.Error(
                "Daily refresh limit exceeded. Resets at $resetsAt",
                SuggestionErrorCode.REFRESH_LIMIT_EXCEEDED
            )
        }

        // Generate new suggestions (without touching daily cache)
        logger.info("Generating refresh suggestions for user {} (refresh #{} today)", 
            username, tracker.refreshCount + 1)
        
        val profile = userProfileAggregator.aggregateUserProfile(userId)
        val prompt = promptBuilder.buildPromptFromProfile(profile)
        val validatedMovies = getValidatedMoviesFromAI(prompt, profile.exclusionList)

        if (validatedMovies.size < REQUIRED_MOVIES) {
            logger.error("Could not get {} valid movies for refresh for user {}", REQUIRED_MOVIES, username)
            return SuggestionResult.Error(
                "Could not generate enough quality recommendations",
                SuggestionErrorCode.VALIDATION_ERROR
            )
        }

        // Increment refresh count
        tracker.refreshCount++
        trackerRepository.save(tracker)
        logger.info("Incremented refresh count for user {} to {}", username, tracker.refreshCount)

        // Build response
        val suggestions = validatedMovies.take(REQUIRED_MOVIES).map { movie ->
            MovieSuggestion(
                id = movie.id,
                title = movie.title,
                posterPath = movie.posterPath,
                rating = movie.rating,
                voteCount = movie.voteCount
            )
        }

        val metadata = buildMetadata(profile)
        val remaining = (limit - tracker.refreshCount).coerceAtLeast(0)
        val resetsAt = LocalDateTime.of(today.plusDays(1), LocalTime.MIDNIGHT)

        return SuggestionResult.Success(
            RefreshSuggestionsResponse(
                suggestions = suggestions,
                generatedAt = today,
                metadata = metadata,
                refreshInfo = RefreshInfo(
                    remainingRefreshes = remaining,
                    dailyLimit = limit,
                    resetsAt = resetsAt
                )
            )
        )
    }

    private fun getOrCreateTracker(user: com.emirhankarci.moviebackend.user.User, date: LocalDate): SuggestionRefreshTracker {
        return trackerRepository.findByUserIdAndRefreshDate(user.id!!, date)
            .orElseGet {
                val newTracker = SuggestionRefreshTracker(
                    user = user,
                    refreshDate = date,
                    refreshCount = 0
                )
                trackerRepository.save(newTracker)
            }
    }

    private fun getValidatedMoviesFromAI(
        prompt: String,
        exclusionList: ExclusionList
    ): List<MovieData> {
        val validatedMovies = mutableListOf<MovieData>()
        var attempts = 0
        val maxAttempts = REQUIRED_MOVIES * MAX_RETRIES_PER_SLOT

        while (validatedMovies.size < REQUIRED_MOVIES && attempts < maxAttempts) {
            attempts++

            val aiResult = aiService.generateSuggestions(prompt)

            when (aiResult) {
                is AiResult.Success -> {
                    val titles = responseParser.parseSuggestionResponse(aiResult.data)

                    for (title in titles) {
                        if (validatedMovies.size >= REQUIRED_MOVIES) break

                        if (exclusionList.containsTitle(title)) {
                            logger.debug("Skipping {} - in exclusion list", title)
                            continue
                        }

                        if (validatedMovies.any { it.title.equals(title, ignoreCase = true) }) {
                            logger.debug("Skipping {} - already validated", title)
                            continue
                        }

                        val movie = tmdbService.searchMovie(title)
                        if (movie != null) {
                            if (exclusionList.containsMovieId(movie.id)) {
                                logger.debug("Skipping {} (ID: {}) - movie ID in exclusion list", title, movie.id)
                                continue
                            }

                            if (tmdbService.validateMovieQuality(movie)) {
                                validatedMovies.add(movie)
                                logger.info("Validated movie: {} (rating: {}, votes: {})",
                                    movie.title, movie.rating, movie.voteCount)
                            } else {
                                logger.debug("Movie {} failed quality validation", title)
                            }
                        } else {
                            logger.debug("Movie {} not found in TMDB", title)
                        }
                    }
                }
                is AiResult.Error -> {
                    logger.error("AI error: {}", aiResult.message)
                }
            }
        }

        return validatedMovies
    }

    private fun buildMetadata(profile: UserProfile): SuggestionMetadata {
        val dataSources = mutableListOf<String>()
        if (profile.watchedMovies.isNotEmpty()) dataSources.add("watched_movies")
        if (profile.watchlistMovies.isNotEmpty()) dataSources.add("watchlist")
        if (profile.preferences != null) dataSources.add("preferences")
        if (profile.likedMovieIds.isNotEmpty() || profile.dislikedMovieIds.isNotEmpty()) {
            dataSources.add("reactions")
        }

        val profileSummary = ProfileSummary(
            watchedCount = profile.watchedMovies.size,
            watchlistCount = profile.watchlistMovies.size,
            hasPreferences = profile.preferences != null,
            topGenres = profile.preferences?.genres?.take(3)
        )

        return SuggestionMetadata(
            personalizationTier = profile.personalizationTier.name,
            dataSources = dataSources,
            profileSummary = profileSummary
        )
    }
}
