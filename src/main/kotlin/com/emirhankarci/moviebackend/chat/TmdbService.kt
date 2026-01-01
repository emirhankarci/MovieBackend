package com.emirhankarci.moviebackend.chat

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class TmdbService(
    private val restTemplate: RestTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TmdbService::class.java)
        private const val TMDB_API_URL = "https://api.themoviedb.org/3"
    }

    private val apiKey: String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    fun searchMovie(movieTitle: String): MovieData? {
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
            
            movie
        } catch (e: Exception) {
            logger.error("TMDB search failed for '{}': {}", movieTitle, e.message)
            null
        }
    }

    private fun searchMovieWithLanguage(movieTitle: String, language: String): MovieData? {
        return try {
            val url = "$TMDB_API_URL/search/movie?api_key=$apiKey&query=${movieTitle.encodeUrl()}&language=$language"
            logger.debug("TMDB search URL: {}", url.replace(apiKey, "***"))
            
            val response = restTemplate.getForObject(url, TmdbSearchResponse::class.java)
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
        return try {
            val url = "$TMDB_API_URL/movie/$movieId?api_key=$apiKey&language=tr-TR"
            val response = restTemplate.getForObject(url, TmdbMovieDetail::class.java)
            
            if (response != null) {
                logger.info("Fetched movie by ID: {} ({})", response.title, movieId)
                MovieData(
                    id = response.id,
                    title = response.title,
                    posterPath = response.poster_path,
                    rating = response.vote_average,
                    voteCount = response.vote_count
                )
            } else {
                logger.warn("Movie not found by ID: {}", movieId)
                null
            }
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

// TMDB Movie Detail Response (for getMovieById)
data class TmdbMovieDetail(
    val id: Long,
    val title: String,
    val poster_path: String?,
    val vote_average: Double,
    val vote_count: Int
)
