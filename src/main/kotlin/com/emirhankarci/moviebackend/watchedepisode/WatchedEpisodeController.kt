package com.emirhankarci.moviebackend.watchedepisode

import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tv/watched")
class WatchedEpisodeController(
    private val watchedEpisodeService: WatchedEpisodeService,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WatchedEpisodeController::class.java)
    }

    @PostMapping("/episode")
    fun markEpisodeWatched(@RequestBody request: MarkEpisodeWatchedRequest): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("POST /api/tv/watched/episode - S{}E{}", request.seasonNumber, request.episodeNumber)

        return when (val result = watchedEpisodeService.markEpisodeWatched(user, request)) {
            is WatchedEpisodeResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.data as Any)
            is WatchedEpisodeResult.Error -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @DeleteMapping("/episode")
    fun unmarkEpisodeWatched(@RequestBody request: UnmarkEpisodeRequest): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("DELETE /api/tv/watched/episode - S{}E{}", request.seasonNumber, request.episodeNumber)

        return when (val result = watchedEpisodeService.unmarkEpisodeWatched(user, request)) {
            is WatchedEpisodeResult.Success -> ResponseEntity.noContent().build()
            is WatchedEpisodeResult.Error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @PostMapping("/season")
    fun markSeasonWatched(@RequestBody request: MarkSeasonWatchedRequest): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("POST /api/tv/watched/season - Season {}", request.seasonNumber)

        return when (val result = watchedEpisodeService.markSeasonWatched(user, request)) {
            is WatchedEpisodeResult.Success -> ResponseEntity.status(HttpStatus.CREATED)
                .body(mapOf("addedEpisodes" to result.data) as Any)
            is WatchedEpisodeResult.Error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @DeleteMapping("/season")
    fun unmarkSeasonWatched(@RequestBody request: UnmarkSeasonRequest): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("DELETE /api/tv/watched/season - Season {}", request.seasonNumber)

        return when (val result = watchedEpisodeService.unmarkSeasonWatched(user, request)) {
            is WatchedEpisodeResult.Success -> ResponseEntity.ok(mapOf("removedEpisodes" to result.data) as Any)
            is WatchedEpisodeResult.Error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @GetMapping("/{seriesId}")
    fun getWatchedEpisodes(@PathVariable seriesId: Long): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/watched/{}", seriesId)

        val response = watchedEpisodeService.getWatchedEpisodes(user, seriesId)
        return if (response != null) {
            ResponseEntity.ok(response as Any)
        } else {
            ResponseEntity.ok(WatchedEpisodesResponse(seriesId, "", emptyList()) as Any)
        }
    }

    @GetMapping("/{seriesId}/progress")
    fun getWatchProgress(
        @PathVariable seriesId: Long,
        @RequestParam(required = false) seasonEpisodeCounts: List<Int>?
    ): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/watched/{}/progress", seriesId)

        // Convert list to map where index+1 is season number (season 1 = index 0)
        val parsedCounts = seasonEpisodeCounts?.mapIndexed { index, count -> (index + 1) to count }?.toMap() ?: emptyMap()
        val response = watchedEpisodeService.getWatchProgress(user, seriesId, parsedCounts)
        return ResponseEntity.ok(response as Any)
    }

    @GetMapping("/status/{seriesId}/{seasonNumber}/{episodeNumber}")
    fun checkEpisodeStatus(
        @PathVariable seriesId: Long,
        @PathVariable seasonNumber: Int,
        @PathVariable episodeNumber: Int
    ): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/watched/status/{}/{}/{}", seriesId, seasonNumber, episodeNumber)

        val response = watchedEpisodeService.checkEpisodeStatus(user, seriesId, seasonNumber, episodeNumber)
        return ResponseEntity.ok(response as Any)
    }

    private fun getCurrentUser() = SecurityContextHolder.getContext().authentication?.name
        ?.let { userRepository.findByUsername(it) }

    private fun unauthorized(): ResponseEntity<Any> = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(mapOf("error" to "UNAUTHORIZED", "message" to "Yetkilendirme gerekli") as Any)
}
