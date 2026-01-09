package com.emirhankarci.moviebackend.tvsuggestion

import com.emirhankarci.moviebackend.chat.AiResult
import com.emirhankarci.moviebackend.chat.AiService
import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class DailyTvSuggestionService(
    private val suggestionRepository: DailyTvSuggestionRepository,
    private val userRepository: UserRepository,
    private val aiService: AiService,
    private val tmdbTvService: TmdbTvService,
    private val promptBuilder: TvSuggestionPromptBuilder,
    private val responseParser: TvSuggestionResponseParser,
    private val tvUserProfileAggregator: TvUserProfileAggregator
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DailyTvSuggestionService::class.java)
        private const val REQUIRED_TV_SERIES = 4
        private const val MAX_RETRIES_PER_SLOT = 3
    }

    fun getDailyTvSuggestions(username: String): TvSuggestionResult<DailyTvSuggestionsResponse> {
        val user = userRepository.findByUsername(username)
            ?: return TvSuggestionResult.Error("User not found", TvSuggestionErrorCode.USER_NOT_FOUND)

        val today = LocalDate.now()
        val userId = user.id!!

        // Check cache first
        val cachedSuggestion = suggestionRepository.findByUserIdAndSuggestionDate(userId, today)

        return if (cachedSuggestion.isPresent) {
            logger.info("Returning cached TV suggestions for user {} on {}", username, today)
            returnCachedSuggestions(cachedSuggestion.get())
        } else {
            logger.info("Generating new TV suggestions for user {} on {}", username, today)
            generateAndSaveSuggestions(userId, username, today)
        }
    }

    private fun returnCachedSuggestions(suggestion: DailyTvSuggestion): TvSuggestionResult<DailyTvSuggestionsResponse> {
        val seriesIds = suggestion.tvSeriesIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        val tvSeries = seriesIds.mapNotNull { tmdbTvService.getTvSeriesById(it) }

        if (tvSeries.size < REQUIRED_TV_SERIES) {
            logger.warn("Some cached TV series could not be fetched from TMDB")
        }

        val suggestions = tvSeries.map { series ->
            TvSuggestion(
                id = series.id,
                name = series.name,
                posterPath = series.posterPath,
                rating = series.rating,
                voteCount = series.voteCount
            )
        }

        return TvSuggestionResult.Success(
            DailyTvSuggestionsResponse(
                suggestions = suggestions,
                cached = true,
                generatedAt = suggestion.suggestionDate
            )
        )
    }

    @Transactional
    private fun generateAndSaveSuggestions(
        userId: Long,
        username: String,
        today: LocalDate
    ): TvSuggestionResult<DailyTvSuggestionsResponse> {
        // Aggregate TV user profile with all data sources
        val profile = tvUserProfileAggregator.aggregateTvUserProfile(userId)
        logger.info(
            "User {} TV profile: tier={}, watched={}, watchlist={}, hasPrefs={}",
            username, profile.personalizationTier, profile.watchedTvSeries.size,
            profile.tvWatchlist.size, profile.preferences != null
        )

        // Build prompt based on full profile
        val prompt = promptBuilder.buildPromptFromProfile(profile)

        // Get AI recommendations with enhanced exclusion
        val validatedSeries = getValidatedTvSeriesFromAI(prompt, profile.exclusionList)

        if (validatedSeries.size < REQUIRED_TV_SERIES) {
            logger.error("Could not get {} valid TV series for user {}", REQUIRED_TV_SERIES, username)
            return TvSuggestionResult.Error(
                "Could not generate enough quality recommendations",
                TvSuggestionErrorCode.VALIDATION_ERROR
            )
        }

        // Save to database
        val seriesIds = validatedSeries.take(REQUIRED_TV_SERIES).joinToString(",") { it.id.toString() }
        val user = userRepository.findByUsername(username)!!

        val suggestion = DailyTvSuggestion(
            user = user,
            tvSeriesIds = seriesIds,
            suggestionDate = today
        )
        suggestionRepository.save(suggestion)
        logger.info("Saved daily TV suggestions for user {}: {}", username, seriesIds)

        // Build response with metadata
        val suggestions = validatedSeries.take(REQUIRED_TV_SERIES).map { series ->
            TvSuggestion(
                id = series.id,
                name = series.name,
                posterPath = series.posterPath,
                rating = series.rating,
                voteCount = series.voteCount
            )
        }

        val metadata = buildMetadata(profile)

        return TvSuggestionResult.Success(
            DailyTvSuggestionsResponse(
                suggestions = suggestions,
                cached = false,
                generatedAt = today,
                metadata = metadata
            )
        )
    }

    private fun getValidatedTvSeriesFromAI(
        prompt: String,
        exclusionList: TvExclusionList
    ): List<TvSeriesData> {
        val validatedSeries = mutableListOf<TvSeriesData>()
        var attempts = 0
        val maxAttempts = REQUIRED_TV_SERIES * MAX_RETRIES_PER_SLOT

        while (validatedSeries.size < REQUIRED_TV_SERIES && attempts < maxAttempts) {
            attempts++

            val aiResult = aiService.generateSuggestions(prompt)

            when (aiResult) {
                is AiResult.Success -> {
                    val titles = responseParser.parseSuggestionResponse(aiResult.data)

                    for (title in titles) {
                        if (validatedSeries.size >= REQUIRED_TV_SERIES) break

                        // Skip if in exclusion list (watched, watchlist, or disliked)
                        if (exclusionList.containsTitle(title)) {
                            logger.debug("Skipping {} - in exclusion list", title)
                            continue
                        }

                        // Skip if already validated in this session
                        if (validatedSeries.any { it.name.equals(title, ignoreCase = true) }) {
                            logger.debug("Skipping {} - already validated", title)
                            continue
                        }

                        // Search and validate
                        val series = tmdbTvService.searchTvSeries(title)
                        if (series != null) {
                            // Additional check: exclude by series ID
                            if (exclusionList.containsSeriesId(series.id)) {
                                logger.debug("Skipping {} (ID: {}) - series ID in exclusion list", title, series.id)
                                continue
                            }

                            if (tmdbTvService.validateTvSeriesQuality(series)) {
                                validatedSeries.add(series)
                                logger.info(
                                    "Validated TV series: {} (rating: {}, votes: {})",
                                    series.name, series.rating, series.voteCount
                                )
                            } else {
                                logger.debug("TV series {} failed quality validation", title)
                            }
                        } else {
                            logger.debug("TV series {} not found in TMDB", title)
                        }
                    }
                }
                is AiResult.Error -> {
                    logger.error("AI error: {}", aiResult.message)
                }
            }
        }

        return validatedSeries
    }

    private fun buildMetadata(profile: TvUserProfile): TvSuggestionMetadata {
        val dataSources = mutableListOf<String>()
        if (profile.watchedTvSeries.isNotEmpty()) dataSources.add("watched_tv_series")
        if (profile.tvWatchlist.isNotEmpty()) dataSources.add("tv_watchlist")
        if (profile.preferences != null) dataSources.add("preferences")
        if (profile.likedTvSeriesIds.isNotEmpty() || profile.dislikedTvSeriesIds.isNotEmpty()) {
            dataSources.add("reactions")
        }

        val profileSummary = TvProfileSummary(
            watchedCount = profile.watchedTvSeries.size,
            watchlistCount = profile.tvWatchlist.size,
            hasPreferences = profile.preferences != null,
            topGenres = profile.preferences?.genres?.take(3)
        )

        return TvSuggestionMetadata(
            personalizationTier = profile.personalizationTier.name,
            dataSources = dataSources,
            profileSummary = profileSummary
        )
    }
}
