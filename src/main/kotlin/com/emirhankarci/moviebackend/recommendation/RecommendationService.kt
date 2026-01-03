package com.emirhankarci.moviebackend.recommendation

import com.emirhankarci.moviebackend.search.TmdbGenreMapper
import com.emirhankarci.moviebackend.user.UserRepository
import com.emirhankarci.moviebackend.watched.WatchedMovie
import com.emirhankarci.moviebackend.watched.WatchedMovieRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import kotlin.random.Random

@Service
class RecommendationService(
    private val watchedMovieRepository: WatchedMovieRepository,
    private val userRepository: UserRepository,
    private val restTemplate: RestTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RecommendationService::class.java)
        private const val TMDB_API_URL = "https://api.themoviedb.org/3"
        private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
        
        const val MIN_RATING = 6.5
        const val STRICT_VOTE_COUNT = 200
        const val RELAXED_VOTE_COUNT = 100
        const val MIN_MOVIES_FOR_STRICT = 3
        const val DEFAULT_LIMIT = 10
        const val MIN_LIMIT = 3
        const val MAX_LIMIT = 15
    }

    private val apiKey: String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    /**
     * Get personalized recommendations for a user
     */
    fun getRecommendationsForUser(username: String, limit: Int): RecommendationResult {
        val user = userRepository.findByUsername(username)
            ?: return RecommendationResult.Error("USER_NOT_FOUND", "Kullanıcı bulunamadı")

        val watchedMovies = watchedMovieRepository.findByUserId(user.id!!)
        
        if (watchedMovies.isEmpty()) {
            logger.info("User {} has no watched movies", username)
            return RecommendationResult.Empty("Henüz izlediğiniz film yok")
        }

        return try {
            val sourceMovie = selectRandomSourceMovie(watchedMovies)
            logger.info("Selected source movie: {} (ID: {}) for user {}", 
                sourceMovie.movieTitle, sourceMovie.movieId, username)

            val tmdbRecommendations = fetchTmdbRecommendations(sourceMovie.movieId)
            val watchedMovieIds = watchedMovies.map { it.movieId }.toSet()
            
            val filteredRecommendations = tmdbRecommendations
                .let { excludeWatchedMovies(it, watchedMovieIds) }
                .let { applyQualityFilter(it) }

            val response = transformToResponse(sourceMovie, filteredRecommendations, clampLimit(limit))
            logger.info("Returning {} recommendations based on {} for user {}", 
                response.recommendations.size, sourceMovie.movieTitle, username)
            
            RecommendationResult.Success(response)
        } catch (e: ResourceAccessException) {
            logger.error("TMDB API unavailable: {}", e.message)
            RecommendationResult.Error("EXTERNAL_SERVICE_ERROR", "Film servisi şu anda kullanılamıyor")
        } catch (e: HttpClientErrorException) {
            logger.error("TMDB API error: {}", e.message)
            RecommendationResult.Error("EXTERNAL_SERVICE_ERROR", "Film servisi hatası")
        } catch (e: Exception) {
            logger.error("Failed to get recommendations for user {}: {}", username, e.message, e)
            RecommendationResult.Error("INTERNAL_ERROR", "Beklenmeyen bir hata oluştu")
        }
    }


    /**
     * Randomly select a source movie from watched movies list
     */
    internal fun selectRandomSourceMovie(watchedMovies: List<WatchedMovie>): WatchedMovie {
        require(watchedMovies.isNotEmpty()) { "Watched movies list cannot be empty" }
        return watchedMovies[Random.nextInt(watchedMovies.size)]
    }

    /**
     * Fetch recommendations from TMDB API for a given movie
     */
    internal fun fetchTmdbRecommendations(movieId: Long): List<TmdbRecommendedMovie> {
        val url = "$TMDB_API_URL/movie/$movieId/recommendations?api_key=$apiKey&language=tr-TR"
        logger.debug("Fetching TMDB recommendations: {}", url.replace(apiKey, "***"))
        
        val response = restTemplate.getForObject(url, TmdbRecommendationsResponse::class.java)
        val results = response?.results ?: emptyList()
        
        logger.debug("TMDB returned {} recommendations for movie {}", results.size, movieId)
        return results
    }

    /**
     * Apply quality filter with relaxation if needed
     */
    internal fun applyQualityFilter(movies: List<TmdbRecommendedMovie>): List<TmdbRecommendedMovie> {
        // First try strict filtering
        val strictFiltered = movies.filter { 
            it.vote_average >= MIN_RATING && it.vote_count >= STRICT_VOTE_COUNT 
        }
        
        if (strictFiltered.size >= MIN_MOVIES_FOR_STRICT) {
            logger.debug("Strict quality filter: {} movies passed", strictFiltered.size)
            return strictFiltered
        }
        
        // Relax vote count threshold
        val relaxedFiltered = movies.filter { 
            it.vote_average >= MIN_RATING && it.vote_count >= RELAXED_VOTE_COUNT 
        }
        
        logger.debug("Relaxed quality filter: {} movies passed (strict had {})", 
            relaxedFiltered.size, strictFiltered.size)
        return relaxedFiltered
    }

    /**
     * Exclude movies that user has already watched
     */
    internal fun excludeWatchedMovies(
        recommendations: List<TmdbRecommendedMovie>,
        watchedMovieIds: Set<Long>
    ): List<TmdbRecommendedMovie> {
        return recommendations.filter { movie ->
            val isWatched = watchedMovieIds.contains(movie.id)
            if (isWatched) {
                logger.debug("Excluding already watched movie: {} (ID: {})", movie.title, movie.id)
            }
            !isWatched
        }
    }

    /**
     * Transform TMDB recommendations to response format
     */
    internal fun transformToResponse(
        sourceMovie: WatchedMovie,
        recommendations: List<TmdbRecommendedMovie>,
        limit: Int
    ): RecommendationsResponse {
        val recommendedMovies = recommendations.take(limit).map { movie ->
            RecommendedMovie(
                id = movie.id,
                title = movie.title,
                posterPath = movie.poster_path?.let { "$TMDB_IMAGE_BASE_URL$it" },
                rating = movie.vote_average,
                releaseYear = extractYear(movie.release_date),
                genres = movie.genre_ids?.map { TmdbGenreMapper.getGenreName(it) } ?: emptyList()
            )
        }

        return RecommendationsResponse(
            sourceMovieTitle = sourceMovie.movieTitle,
            displayMessage = buildDisplayMessage(sourceMovie.movieTitle),
            recommendations = recommendedMovies
        )
    }

    /**
     * Clamp limit to valid range [MIN_LIMIT, MAX_LIMIT]
     */
    internal fun clampLimit(limit: Int): Int {
        return limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
    }

    /**
     * Build display message for the recommendations section
     */
    internal fun buildDisplayMessage(sourceMovieTitle: String): String {
        return "$sourceMovieTitle izledin, bunları da izleyebilirsin"
    }

    /**
     * Extract year from release date string (YYYY-MM-DD format)
     */
    private fun extractYear(releaseDate: String?): Int {
        return releaseDate?.take(4)?.toIntOrNull() ?: 0
    }
}
