package com.emirhankarci.moviebackend.chat

import com.emirhankarci.moviebackend.cache.CacheKeys
import com.emirhankarci.moviebackend.cache.CacheService
import com.emirhankarci.moviebackend.tmdb.TmdbApiClient
import com.emirhankarci.moviebackend.tmdb.dto.MovieDetailResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TmdbService(
    private val tmdbApiClient: TmdbApiClient,
    private val cacheService: CacheService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TmdbService::class.java)
    }

    fun searchMovie(movieTitle: String): MovieData? {
        val cacheKey = CacheKeys.Chat.search(movieTitle)
        
        // Check Redis cache first
        cacheService.get(cacheKey, MovieData::class.java)?.let { cached ->
            logger.info("Redis cache HIT for chat search: {}", movieTitle)
            return cached
        }
        
        logger.info("Redis cache MISS for chat search: {}, fetching from TMDB", movieTitle)
        
        return try {
            // First try with Turkish language
            var movie = searchMovieWithLanguage(movieTitle, "tr-TR")
            
            // If not found, try with English
            if (movie == null) {
                logger.info("Movie not found in Turkish, trying English for: {}", movieTitle)
                movie = searchMovieWithLanguage(movieTitle, "en-US")
            }
            
            // If still not found, try without year/extra info
            if (movie == null && movieTitle.contains("(")) {
                val cleanTitle = movieTitle.substringBefore("(").trim()
                logger.info("Trying with cleaned title: {}", cleanTitle)
                movie = searchMovieWithLanguage(cleanTitle, "en-US")
            }
            
            // Cache the result if found
            movie?.let {
                cacheService.set(cacheKey, it, CacheKeys.TTL.SHORT)
            }
            
            movie
        } catch (e: Exception) {
            logger.error("TMDB search failed for '{}': {}", movieTitle, e.message)
            null
        }
    }

    private fun searchMovieWithLanguage(movieTitle: String, language: String): MovieData? {
        return try {
            val url = "https://api.themoviedb.org/3/search/movie?api_key=${getApiKey()}&query=${movieTitle.encodeUrl()}&language=$language"
            logger.debug("TMDB search URL: {}", url.replace(getApiKey(), "***"))
            
            val response = org.springframework.web.client.RestTemplate().getForObject(url, TmdbSearchResponse::class.java)
            logger.debug("TMDB response: {} results", response?.results?.size ?: 0)
            
            val movie = response?.results?.firstOrNull()
            if (movie != null) {
                logger.info("Found movie: {} (ID: {}) with language {}", movie.title, movie.id, language)
                MovieData(
                    id = movie.id,
                    title = movie.title,
                    posterPath = movie.poster_path,
                    rating = movie.vote_average,
                    voteCount = movie.vote_count
                )
            } else {
                logger.debug("Movie not found with language {}: {}", language, movieTitle)
                null
            }
        } catch (e: Exception) {
            logger.error("TMDB search failed for '{}' ({}): {}", movieTitle, language, e.message)
            null
        }
    }

    fun getMovieById(movieId: Long): MovieData? {
        val cacheKey = CacheKeys.Movie.detail(movieId)
        
        // Try to get from existing movie detail cache
        cacheService.get(cacheKey, MovieDetailResponse::class.java)?.let { cached ->
            logger.info("Redis cache HIT for movie detail (chat): {}", movieId)
            return MovieData(
                id = cached.id,
                title = cached.title,
                posterPath = cached.posterPath,
                rating = cached.voteAverage,
                voteCount = 0 // Not available in MovieDetailResponse
            )
        }
        
        return try {
            val detail = tmdbApiClient.getMovieDetail(movieId)
            logger.info("Fetched movie by ID: {} ({})", detail.title, movieId)
            
            MovieData(
                id = detail.id,
                title = detail.title,
                posterPath = tmdbApiClient.buildPosterUrl(detail.poster_path),
                rating = detail.vote_average,
                voteCount = detail.vote_count
            )
        } catch (e: Exception) {
            logger.error("TMDB fetch by ID failed for '{}': {}", movieId, e.message)
            null
        }
    }

    fun validateMovieQuality(movie: MovieData): Boolean {
        val isValid = movie.rating > 6.49 && movie.voteCount > 1000
        if (!isValid) {
            logger.info("Movie {} failed quality check: rating={}, votes={} (need >6.49 rating, >1000 votes)", 
                movie.title, movie.rating, movie.voteCount)
        } else {
            logger.info("Movie {} passed quality check: rating={}, votes={}", 
                movie.title, movie.rating, movie.voteCount)
        }
        return isValid
    }

    private fun getApiKey(): String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }
}

// TMDB API Response models
data class TmdbSearchResponse(
    val results: List<TmdbMovie>?
)

data class TmdbMovie(
    val id: Long,
    val title: String,
    val poster_path: String?,
    val vote_average: Double,
    val vote_count: Int
)

// Our MovieData model
data class MovieData(
    val id: Long,
    val title: String,
    val posterPath: String?,
    val rating: Double,
    val voteCount: Int
)
