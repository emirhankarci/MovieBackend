package com.emirhankarci.moviebackend.tmdb

import com.emirhankarci.moviebackend.tmdb.dto.*
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Movie Controller
 * Film verilerini döndüren REST API endpoint'leri.
 */
@RestController
@RequestMapping("/api/movies")
class MovieController(
    private val tmdbMovieService: TmdbMovieService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MovieController::class.java)
    }

    /**
     * GET /api/movies/popular
     * Popüler filmleri getirir
     * 
     * @param page Sayfa numarası (default: 1)
     */
    @GetMapping("/popular")
    fun getPopularMovies(
        @RequestParam(defaultValue = "1") page: Int
    ): ResponseEntity<PopularMoviesResponse> {
        logger.info("GET /api/movies/popular - page: {}", page)
        
        if (page < 1) {
            throw TmdbApiException("Page must be greater than 0", 400)
        }
        
        val response = tmdbMovieService.getPopularMovies(page)
        return ResponseEntity.ok(response)
    }

    /**
     * GET /api/movies/{movieId}
     * Film detaylarını getirir
     * 
     * @param movieId Film ID
     */
    @GetMapping("/{movieId}")
    fun getMovieDetail(
        @PathVariable movieId: Long
    ): ResponseEntity<MovieDetailResponse> {
        logger.info("GET /api/movies/{}", movieId)
        
        if (movieId < 1) {
            throw TmdbApiException("Invalid movie ID", 400)
        }
        
        val response = tmdbMovieService.getMovieDetail(movieId)
        return ResponseEntity.ok(response)
    }

    /**
     * GET /api/movies/{movieId}/credits
     * Film kadrosunu getirir
     * 
     * @param movieId Film ID
     * @param limit Maksimum oyuncu sayısı (default: 20)
     */
    @GetMapping("/{movieId}/credits")
    fun getMovieCredits(
        @PathVariable movieId: Long,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<MovieCreditsResponse> {
        logger.info("GET /api/movies/{}/credits - limit: {}", movieId, limit)
        
        if (movieId < 1) {
            throw TmdbApiException("Invalid movie ID", 400)
        }
        
        val effectiveLimit = limit.coerceIn(1, 50)
        val response = tmdbMovieService.getMovieCredits(movieId, effectiveLimit)
        return ResponseEntity.ok(response)
    }

    /**
     * GET /api/movies/{movieId}/recommendations
     * Film önerilerini getirir
     * 
     * @param movieId Film ID
     * @param limit Maksimum öneri sayısı (default: 10)
     */
    @GetMapping("/{movieId}/recommendations")
    fun getMovieRecommendations(
        @PathVariable movieId: Long,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<MovieRecommendationsResponse> {
        logger.info("GET /api/movies/{}/recommendations - limit: {}", movieId, limit)
        
        if (movieId < 1) {
            throw TmdbApiException("Invalid movie ID", 400)
        }
        
        val effectiveLimit = limit.coerceIn(1, 20)
        val response = tmdbMovieService.getMovieRecommendations(movieId, effectiveLimit)
        return ResponseEntity.ok(response)
    }
}
