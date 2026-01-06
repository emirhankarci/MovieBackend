package com.emirhankarci.moviebackend.tmdb

import com.emirhankarci.moviebackend.resilience.CircuitBreakerService
import com.emirhankarci.moviebackend.tmdb.model.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

/**
 * TMDB API Client
 * TMDB API'sine HTTP çağrıları yapan client.
 * Circuit Breaker ile korunur.
 */
@Component
class TmdbApiClient(
    private val restTemplate: RestTemplate,
    private val circuitBreakerService: CircuitBreakerService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TmdbApiClient::class.java)
        private const val BASE_URL = "https://api.themoviedb.org/3"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p"
        const val POSTER_SIZE = "w500"
        const val BACKDROP_SIZE = "w780"
        const val PROFILE_SIZE = "w185"
        const val STILL_SIZE = "w300"
    }

    private val apiKey: String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    /**
     * Popüler filmleri getirir
     */
    fun getPopularMovies(page: Int = 1, language: String = "tr-TR"): TmdbPopularResponse {
        val url = "$BASE_URL/movie/popular?api_key=$apiKey&language=$language&page=$page"
        logger.debug("Fetching popular movies: page={}, language={}", page, language)
        
        return executeRequest(url, TmdbPopularResponse::class.java)
            ?: throw TmdbApiException("Failed to fetch popular movies", HttpStatus.SERVICE_UNAVAILABLE.value())
    }

    /**
     * Film detaylarını getirir
     */
    fun getMovieDetail(movieId: Long, language: String = "tr-TR"): TmdbMovieDetailResponse {
        val url = "$BASE_URL/movie/$movieId?api_key=$apiKey&language=$language"
        logger.debug("Fetching movie detail: movieId={}, language={}", movieId, language)
        
        return executeRequest(url, TmdbMovieDetailResponse::class.java)
            ?: throw TmdbApiException("Movie not found: $movieId", HttpStatus.NOT_FOUND.value())
    }

    /**
     * Film kadrosunu getirir
     */
    fun getMovieCredits(movieId: Long): TmdbCreditsResponse {
        val url = "$BASE_URL/movie/$movieId/credits?api_key=$apiKey"
        logger.debug("Fetching movie credits: movieId={}", movieId)
        
        return executeRequest(url, TmdbCreditsResponse::class.java)
            ?: throw TmdbApiException("Credits not found for movie: $movieId", HttpStatus.NOT_FOUND.value())
    }

    /**
     * Film önerilerini getirir
     */
    fun getMovieRecommendations(movieId: Long, language: String = "tr-TR"): TmdbRecommendationsResponse {
        val url = "$BASE_URL/movie/$movieId/recommendations?api_key=$apiKey&language=$language"
        logger.debug("Fetching movie recommendations: movieId={}, language={}", movieId, language)
        
        return executeRequest(url, TmdbRecommendationsResponse::class.java)
            ?: throw TmdbApiException("Recommendations not found for movie: $movieId", HttpStatus.NOT_FOUND.value())
    }

    // ==================== Actor Methods ====================

    /**
     * Oyuncu detaylarını getirir
     */
    fun getActorDetail(actorId: Long, language: String = "tr-TR"): TmdbActorDetailResponse {
        val url = "$BASE_URL/person/$actorId?api_key=$apiKey&language=$language"
        logger.debug("Fetching actor detail: actorId={}, language={}", actorId, language)
        
        return executeRequest(url, TmdbActorDetailResponse::class.java)
            ?: throw TmdbApiException("Actor not found: $actorId", HttpStatus.NOT_FOUND.value())
    }

    /**
     * Oyuncunun film kredilerini getirir
     */
    fun getActorMovieCredits(actorId: Long, language: String = "tr-TR"): TmdbActorMovieCreditsResponse {
        val url = "$BASE_URL/person/$actorId/movie_credits?api_key=$apiKey&language=$language"
        logger.debug("Fetching actor movie credits: actorId={}, language={}", actorId, language)
        
        return executeRequest(url, TmdbActorMovieCreditsResponse::class.java)
            ?: throw TmdbApiException("Movie credits not found for actor: $actorId", HttpStatus.NOT_FOUND.value())
    }

    /**
     * Poster path'i tam URL'e çevirir
     */
    fun buildPosterUrl(path: String?): String? {
        return path?.let { "$IMAGE_BASE_URL/$POSTER_SIZE$it" }
    }

    /**
     * Backdrop path'i tam URL'e çevirir
     */
    fun buildBackdropUrl(path: String?): String? {
        return path?.let { "$IMAGE_BASE_URL/$BACKDROP_SIZE$it" }
    }

    /**
     * Profile path'i tam URL'e çevirir
     */
    fun buildProfileUrl(path: String?): String? {
        return path?.let { "$IMAGE_BASE_URL/$PROFILE_SIZE$it" }
    }

    /**
     * Still path'i tam URL'e çevirir (bölüm görselleri için)
     */
    fun buildStillUrl(path: String?): String? {
        return path?.let { "$IMAGE_BASE_URL/$STILL_SIZE$it" }
    }

    // ==================== TV Series Methods ====================

    /**
     * TV dizisi detaylarını getirir
     */
    fun getTvSeriesDetail(seriesId: Long, language: String = "tr-TR"): TmdbTvSeriesDetailResponse {
        val url = "$BASE_URL/tv/$seriesId?api_key=$apiKey&language=$language"
        logger.debug("Fetching TV series detail: seriesId={}, language={}", seriesId, language)
        
        return executeRequest(url, TmdbTvSeriesDetailResponse::class.java)
            ?: throw TmdbApiException("TV series not found: $seriesId", HttpStatus.NOT_FOUND.value())
    }

    /**
     * TV dizisi sezon detaylarını getirir
     */
    fun getTvSeasonDetail(seriesId: Long, seasonNumber: Int, language: String = "tr-TR"): TmdbSeasonDetailResponse {
        val url = "$BASE_URL/tv/$seriesId/season/$seasonNumber?api_key=$apiKey&language=$language"
        logger.debug("Fetching TV season detail: seriesId={}, seasonNumber={}, language={}", seriesId, seasonNumber, language)
        
        return executeRequest(url, TmdbSeasonDetailResponse::class.java)
            ?: throw TmdbApiException("Season not found: series=$seriesId, season=$seasonNumber", HttpStatus.NOT_FOUND.value())
    }

    /**
     * TV dizisi bölüm detaylarını getirir
     */
    fun getTvEpisodeDetail(seriesId: Long, seasonNumber: Int, episodeNumber: Int, language: String = "tr-TR"): TmdbEpisodeDetailResponse {
        val url = "$BASE_URL/tv/$seriesId/season/$seasonNumber/episode/$episodeNumber?api_key=$apiKey&language=$language"
        logger.debug("Fetching TV episode detail: seriesId={}, seasonNumber={}, episodeNumber={}, language={}", seriesId, seasonNumber, episodeNumber, language)
        
        return executeRequest(url, TmdbEpisodeDetailResponse::class.java)
            ?: throw TmdbApiException("Episode not found: series=$seriesId, season=$seasonNumber, episode=$episodeNumber", HttpStatus.NOT_FOUND.value())
    }

    /**
     * TV dizisi kadrosunu getirir
     */
    fun getTvSeriesCredits(seriesId: Long): TmdbTvCreditsResponse {
        val url = "$BASE_URL/tv/$seriesId/credits?api_key=$apiKey"
        logger.debug("Fetching TV series credits: seriesId={}", seriesId)
        
        return executeRequest(url, TmdbTvCreditsResponse::class.java)
            ?: throw TmdbApiException("Credits not found for TV series: $seriesId", HttpStatus.NOT_FOUND.value())
    }

    private fun <T : Any> executeRequest(url: String, responseType: Class<T>): T? {
        return circuitBreakerService.executeWithTmdbCircuitBreaker(
            fallback = {
                logger.warn("TMDB circuit breaker OPEN - returning null for: {}", url.maskApiKey())
                null
            },
            supplier = {
                try {
                    logger.debug("TMDB API call: {}", url.maskApiKey())
                    val response = restTemplate.getForObject(url, responseType, emptyMap<String, Any>())
                    logger.debug("TMDB API call successful")
                    response
                } catch (e: HttpClientErrorException) {
                    logger.error("TMDB API error: {} - {}", e.statusCode, e.message)
                    when (e.statusCode) {
                        HttpStatus.NOT_FOUND -> throw TmdbApiException("Resource not found", 404)
                        HttpStatus.TOO_MANY_REQUESTS -> throw TmdbApiException("Rate limit exceeded", 429)
                        HttpStatus.UNAUTHORIZED -> throw TmdbApiException("Invalid API key", 401)
                        else -> throw TmdbApiException("TMDB API error: ${e.message}", e.statusCode.value())
                    }
                } catch (e: RestClientException) {
                    logger.error("TMDB API connection error: {}", e.message)
                    throw TmdbApiException("TMDB service unavailable: ${e.message}", 503)
                }
            }
        )
    }

    private fun String.maskApiKey(): String {
        return this.replace(apiKey, "***")
    }
}
