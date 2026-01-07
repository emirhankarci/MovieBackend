package com.emirhankarci.moviebackend.tvrating

import com.emirhankarci.moviebackend.common.PageResponse
import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tv/rating")
class TvRatingController(
    private val tvRatingService: TvRatingService,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvRatingController::class.java)
    }

    // Series Rating Endpoints
    @PostMapping("/series")
    fun rateSeries(@RequestBody request: RateSeriesRequest): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("POST /api/tv/rating/series - seriesId: {}", request.seriesId)

        return when (val result = tvRatingService.rateSeries(user, request)) {
            is TvRatingResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.data as Any)
            is TvRatingResult.Error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @DeleteMapping("/series/{seriesId}")
    fun removeSeriesRating(@PathVariable seriesId: Long): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("DELETE /api/tv/rating/series/{}", seriesId)

        return when (val result = tvRatingService.removeSeriesRating(user, seriesId)) {
            is TvRatingResult.Success -> ResponseEntity.noContent().build()
            is TvRatingResult.Error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @GetMapping("/series")
    fun getRatedSeries(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/rating/series - page: {}, size: {}", page, size)

        val result = tvRatingService.getRatedSeries(user, page, size)
        return ResponseEntity.ok(PageResponse.from(result) { it } as Any)
    }

    @GetMapping("/series/status/{seriesId}")
    fun getSeriesRatingStatus(@PathVariable seriesId: Long): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/rating/series/status/{}", seriesId)

        val response = tvRatingService.getSeriesRatingStatus(user, seriesId)
        return ResponseEntity.ok(response as Any)
    }

    // Episode Rating Endpoints
    @PostMapping("/episode")
    fun rateEpisode(@RequestBody request: RateEpisodeRequest): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("POST /api/tv/rating/episode - S{}E{}", request.seasonNumber, request.episodeNumber)

        return when (val result = tvRatingService.rateEpisode(user, request)) {
            is TvRatingResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.data as Any)
            is TvRatingResult.Error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @DeleteMapping("/episode")
    fun removeEpisodeRating(@RequestBody request: DeleteEpisodeRatingRequest): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("DELETE /api/tv/rating/episode - S{}E{}", request.seasonNumber, request.episodeNumber)

        return when (val result = tvRatingService.removeEpisodeRating(user, request)) {
            is TvRatingResult.Success -> ResponseEntity.noContent().build()
            is TvRatingResult.Error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @GetMapping("/episode/{seriesId}")
    fun getRatedEpisodes(@PathVariable seriesId: Long): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/rating/episode/{}", seriesId)

        val response = tvRatingService.getRatedEpisodes(user, seriesId)
        return ResponseEntity.ok(response as Any)
    }

    private fun getCurrentUser() = SecurityContextHolder.getContext().authentication?.name
        ?.let { userRepository.findByUsername(it) }

    private fun unauthorized(): ResponseEntity<Any> = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(mapOf("error" to "UNAUTHORIZED", "message" to "Yetkilendirme gerekli") as Any)
}
