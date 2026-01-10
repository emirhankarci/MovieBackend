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
     * Oyuncunun filmografisini getirir (paginated)
     * 
     * @param actorId Oyuncu ID
     * @param page Sayfa numarası (default: 1)
     * @param limit Sayfa başına kayıt (default: 20, max: 50)
     */
    @GetMapping("/{actorId}/filmography")
    fun getActorFilmography(
        @PathVariable actorId: Long,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<PaginatedActorFilmographyResponse> {
        logger.info("GET /api/actors/{}/filmography - page: {}, limit: {}", actorId, page, limit)
        
        if (actorId < 1) {
            throw TmdbApiException("Invalid actor ID", 400)
        }
        
        val effectivePage = maxOf(1, page)
        val effectiveLimit = limit.coerceIn(1, 50)
        val response = tmdbActorService.getActorFilmography(actorId, effectivePage, effectiveLimit)
        return ResponseEntity.ok(response)
    }

    /**
     * GET /api/actors/{actorId}/tv-credits
     * Oyuncunun TV dizi kredilerini getirir (paginated)
     * 
     * @param actorId Oyuncu ID
     * @param page Sayfa numarası (default: 1)
     * @param limit Sayfa başına kayıt (default: 20, max: 50)
     */
    @GetMapping("/{actorId}/tv-credits")
    fun getActorTvCredits(
        @PathVariable actorId: Long,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<PaginatedActorTvCreditsResponse> {
        logger.info("GET /api/actors/{}/tv-credits - page: {}, limit: {}", actorId, page, limit)
        
        if (actorId < 1) {
            throw TmdbApiException("Invalid actor ID", 400)
        }
        
        val effectivePage = maxOf(1, page)
        val effectiveLimit = limit.coerceIn(1, 50)
        val response = tmdbActorService.getActorTvCredits(actorId, effectivePage, effectiveLimit)
        return ResponseEntity.ok(response)
    }

    /**
     * GET /api/actors/{actorId}/external-ids
     * Oyuncunun sosyal medya ID'lerini getirir
     * 
     * @param actorId Oyuncu ID
     */
    @GetMapping("/{actorId}/external-ids")
    fun getActorExternalIds(
        @PathVariable actorId: Long
    ): ResponseEntity<ActorExternalIdsResponse> {
        logger.info("GET /api/actors/{}/external-ids", actorId)
        
        if (actorId < 1) {
            throw TmdbApiException("Invalid actor ID", 400)
        }
        
        val response = tmdbActorService.getActorExternalIds(actorId)
        return ResponseEntity.ok(response)
    }
}
