package com.emirhankarci.moviebackend.tvseries

import com.emirhankarci.moviebackend.tmdb.TmdbApiException
import com.emirhankarci.moviebackend.tvseries.dto.*
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * TV Series Controller
 * TV dizisi verilerini döndüren REST API endpoint'leri.
 */
@RestController
@RequestMapping("/api/tv")
class TvSeriesController(
    private val tvSeriesService: TvSeriesService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvSeriesController::class.java)
    }

    /**
     * GET /api/tv/{seriesId}
     * TV dizisi detaylarını getirir
     */
    @GetMapping("/{seriesId}")
    fun getSeriesDetail(
        @PathVariable seriesId: Long
    ): ResponseEntity<Any> {
        logger.info("GET /api/tv/{}", seriesId)
        
        if (seriesId < 1) {
            return ResponseEntity.badRequest().body(
                TvSeriesErrorResponse(
                    error = "INVALID_PARAMETER",
                    message = "Geçersiz dizi ID'si"
                )
            )
        }
        
        return try {
            val response = tvSeriesService.getSeriesDetail(seriesId)
            ResponseEntity.ok(response)
        } catch (e: TmdbApiException) {
            handleTmdbException(e, "Dizi")
        }
    }

    /**
     * GET /api/tv/{seriesId}/season/{seasonNumber}
     * Sezon detaylarını getirir
     */
    @GetMapping("/{seriesId}/season/{seasonNumber}")
    fun getSeasonDetail(
        @PathVariable seriesId: Long,
        @PathVariable seasonNumber: Int
    ): ResponseEntity<Any> {
        logger.info("GET /api/tv/{}/season/{}", seriesId, seasonNumber)
        
        if (seriesId < 1) {
            return ResponseEntity.badRequest().body(
                TvSeriesErrorResponse(
                    error = "INVALID_PARAMETER",
                    message = "Geçersiz dizi ID'si"
                )
            )
        }
        
        if (seasonNumber < 0) {
            return ResponseEntity.badRequest().body(
                TvSeriesErrorResponse(
                    error = "INVALID_PARAMETER",
                    message = "Geçersiz sezon numarası"
                )
            )
        }
        
        return try {
            val response = tvSeriesService.getSeasonDetail(seriesId, seasonNumber)
            ResponseEntity.ok(response)
        } catch (e: TmdbApiException) {
            handleTmdbException(e, "Sezon")
        }
    }


    /**
     * GET /api/tv/{seriesId}/season/{seasonNumber}/episode/{episodeNumber}
     * Bölüm detaylarını getirir
     */
    @GetMapping("/{seriesId}/season/{seasonNumber}/episode/{episodeNumber}")
    fun getEpisodeDetail(
        @PathVariable seriesId: Long,
        @PathVariable seasonNumber: Int,
        @PathVariable episodeNumber: Int
    ): ResponseEntity<Any> {
        logger.info("GET /api/tv/{}/season/{}/episode/{}", seriesId, seasonNumber, episodeNumber)
        
        if (seriesId < 1) {
            return ResponseEntity.badRequest().body(
                TvSeriesErrorResponse(
                    error = "INVALID_PARAMETER",
                    message = "Geçersiz dizi ID'si"
                )
            )
        }
        
        if (seasonNumber < 0) {
            return ResponseEntity.badRequest().body(
                TvSeriesErrorResponse(
                    error = "INVALID_PARAMETER",
                    message = "Geçersiz sezon numarası"
                )
            )
        }
        
        if (episodeNumber < 1) {
            return ResponseEntity.badRequest().body(
                TvSeriesErrorResponse(
                    error = "INVALID_PARAMETER",
                    message = "Geçersiz bölüm numarası"
                )
            )
        }
        
        return try {
            val response = tvSeriesService.getEpisodeDetail(seriesId, seasonNumber, episodeNumber)
            ResponseEntity.ok(response)
        } catch (e: TmdbApiException) {
            handleTmdbException(e, "Bölüm")
        }
    }

    /**
     * GET /api/tv/{seriesId}/credits
     * TV dizisi kadrosunu getirir
     */
    @GetMapping("/{seriesId}/credits")
    fun getSeriesCredits(
        @PathVariable seriesId: Long,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<Any> {
        logger.info("GET /api/tv/{}/credits - limit: {}", seriesId, limit)
        
        if (seriesId < 1) {
            return ResponseEntity.badRequest().body(
                TvSeriesErrorResponse(
                    error = "INVALID_PARAMETER",
                    message = "Geçersiz dizi ID'si"
                )
            )
        }
        
        val effectiveLimit = limit.coerceIn(1, 50)
        
        return try {
            val response = tvSeriesService.getSeriesCredits(seriesId, effectiveLimit)
            ResponseEntity.ok(response)
        } catch (e: TmdbApiException) {
            handleTmdbException(e, "Kadro")
        }
    }

    /**
     * TMDB exception'larını uygun HTTP response'a çevirir
     */
    private fun handleTmdbException(e: TmdbApiException, resourceType: String): ResponseEntity<Any> {
        logger.error("TMDB API error: {} - {}", e.statusCode, e.message)
        
        return when (e.statusCode) {
            404 -> ResponseEntity.status(404).body(
                TvSeriesErrorResponse(
                    error = "NOT_FOUND",
                    message = "$resourceType bulunamadı"
                )
            )
            503 -> ResponseEntity.status(503).body(
                TvSeriesErrorResponse(
                    error = "EXTERNAL_SERVICE_ERROR",
                    message = "Dizi servisi şu anda kullanılamıyor"
                )
            )
            else -> ResponseEntity.status(503).body(
                TvSeriesErrorResponse(
                    error = "EXTERNAL_SERVICE_ERROR",
                    message = "Dizi servisi şu anda kullanılamıyor"
                )
            )
        }
    }
}
