package com.emirhankarci.moviebackend.suggestion

import com.emirhankarci.moviebackend.chat.*
import com.emirhankarci.moviebackend.movie.WatchlistRepository
import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class DailySuggestionService(
    private val suggestionRepository: DailySuggestionRepository,
    private val watchlistRepository: WatchlistRepository,
    private val userRepository: UserRepository,
    private val aiService: AiService,
    private val tmdbService: TmdbService,
    private val promptBuilder: SuggestionPromptBuilder,
    private val responseParser: SuggestionResponseParser
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DailySuggestionService::class.java)
        private const val REQUIRED_MOVIES = 4
        private const val MAX_RETRIES_PER_SLOT = 3
    }

    fun getDailySuggestions(username: String): SuggestionResult<DailySuggestionsResponse> {
        val user = userRepository.findByUsername(username)
            ?: return SuggestionResult.Error("User not found", SuggestionErrorCode.USER_NOT_FOUND)

        val today = LocalDate.now()
        val userId = user.id!!
        
        // Check cache first
        val cachedSuggestion = suggestionRepository.findByUserIdAndSuggestionDate(userId, today)
        
        return if (cachedSuggestion.isPresent) {
            logger.info("Returning cached suggestions for user {} on {}", username, today)
            returnCachedSuggestions(cachedSuggestion.get())
        } else {
            logger.info("Generating new suggestions for user {} on {}", username, today)
            generateAndSaveSuggestions(userId, username, today)
        }
    }

    private fun returnCachedSuggestions(suggestion: DailySuggestion): SuggestionResult<DailySuggestionsResponse> {
        val movieIds = suggestion.movieIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        val movies = movieIds.mapNotNull { tmdbService.getMovieById(it) }
        
        if (movies.size < REQUIRED_MOVIES) {
            logger.warn("Some cached movies could not be fetched from TMDB")
        }

        val suggestions = movies.map { movie ->
            MovieSuggestion(
                id = movie.id,
                title = movie.title,
                posterPath = movie.posterPath,
                rating = movie.rating,
                voteCount = movie.voteCount
            )
        }

        return SuggestionResult.Success(
            DailySuggestionsResponse(
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
    ): SuggestionResult<DailySuggestionsResponse> {
        // Get user's watchlist for context
        val watchlist = watchlistRepository.findByUserId(userId)
        val watchlistTitles = watchlist.map { it.movieTitle }
        
        // Build prompt based on watchlist
        val prompt = if (watchlistTitles.isEmpty()) {
            logger.info("User {} has empty watchlist, using generic prompt", username)
            promptBuilder.buildGenericPrompt()
        } else {
            logger.info("User {} has {} movies in watchlist", username, watchlistTitles.size)
            promptBuilder.buildPersonalizedPrompt(watchlistTitles)
        }

        // Get AI recommendations
        val validatedMovies = getValidatedMoviesFromAI(prompt, watchlistTitles)
        
        if (validatedMovies.size < REQUIRED_MOVIES) {
            logger.error("Could not get {} valid movies for user {}", REQUIRED_MOVIES, username)
            return SuggestionResult.Error(
                "Could not generate enough quality recommendations",
                SuggestionErrorCode.VALIDATION_ERROR
            )
        }

        // Save to database
        val movieIds = validatedMovies.take(REQUIRED_MOVIES).joinToString(",") { it.id.toString() }
        val user = userRepository.findByUsername(username)!!
        
        val suggestion = DailySuggestion(
            user = user,
            movieIds = movieIds,
            suggestionDate = today
        )
        suggestionRepository.save(suggestion)
        logger.info("Saved daily suggestions for user {}: {}", username, movieIds)

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

        return SuggestionResult.Success(
            DailySuggestionsResponse(
                suggestions = suggestions,
                cached = false,
                generatedAt = today
            )
        )
    }

    private fun getValidatedMoviesFromAI(
        prompt: String,
        excludeTitles: List<String>
    ): List<MovieData> {
        val validatedMovies = mutableListOf<MovieData>()
        var attempts = 0
        val maxAttempts = REQUIRED_MOVIES * MAX_RETRIES_PER_SLOT

        while (validatedMovies.size < REQUIRED_MOVIES && attempts < maxAttempts) {
            attempts++
            
            // Call AI with suggestion-specific method
            val aiResult = aiService.generateSuggestions(prompt)
            
            when (aiResult) {
                is AiResult.Success -> {
                    val titles = responseParser.parseSuggestionResponse(aiResult.data)
                    
                    for (title in titles) {
                        if (validatedMovies.size >= REQUIRED_MOVIES) break
                        
                        // Skip if already in watchlist
                        if (excludeTitles.any { it.equals(title, ignoreCase = true) }) {
                            logger.debug("Skipping {} - already in watchlist", title)
                            continue
                        }
                        
                        // Skip if already validated
                        if (validatedMovies.any { it.title.equals(title, ignoreCase = true) }) {
                            logger.debug("Skipping {} - already validated", title)
                            continue
                        }
                        
                        // Search and validate
                        val movie = tmdbService.searchMovie(title)
                        if (movie != null && tmdbService.validateMovieQuality(movie)) {
                            validatedMovies.add(movie)
                            logger.info("Validated movie: {} (rating: {}, votes: {})", 
                                movie.title, movie.rating, movie.voteCount)
                        } else {
                            logger.debug("Movie {} failed validation or not found", title)
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
}
