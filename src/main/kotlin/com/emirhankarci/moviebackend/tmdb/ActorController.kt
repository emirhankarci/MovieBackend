package com.emirhankarci.moviebackend.tmdb

import com.emirhankarci.moviebackend.tmdb.dto.*
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Actor Controller
 * Oyuncu verilerini döndüren REST API endpoint'leri.
 */
@RestController
@RequestMapping("/api/actors")
class ActorController(
    private val tmdbActorService: TmdbActorService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ActorController::class.java)
    }

    /**
     * GET /api/actors/{actorId}
     * Oyuncu detaylarını getirir
     * 
     * @param actorId Oyuncu ID
     */
    @GetMapping("/{actorId}")
    fun getActorDetail(
        @PathVariable actorId: Long
    ): ResponseEntity<ActorDetailResponse> {
        logger.info("GET /api/actors/{}", actorId)
        
        if (actorId < 1) {
            throw TmdbApiException("Invalid actor ID", 400)
        }
        
        val response = tmdbActorService.getActorDetail(actorId)
        return ResponseEntity.ok(response)
    }

    /**
     * GET /api/actors/{actorId}/filmography
     * Oyuncunun filmografisini getirir
     * 
     * @param actorId Oyuncu ID
     * @param limit Maksimum film sayısı (default: 20)
     */
    @GetMapping("/{actorId}/filmography")
    fun getActorFilmography(
        @PathVariable actorId: Long,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<ActorFilmographyResponse> {
        logger.info("GET /api/actors/{}/filmography - limit: {}", actorId, limit)
        
        if (actorId < 1) {
            throw TmdbApiException("Invalid actor ID", 400)
        }
        
        val effectiveLimit = limit.coerceIn(1, 50)
        val response = tmdbActorService.getActorFilmography(actorId, effectiveLimit)
        return ResponseEntity.ok(response)
    }
}
