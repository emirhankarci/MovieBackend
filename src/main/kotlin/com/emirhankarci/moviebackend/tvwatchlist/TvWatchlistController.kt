package com.emirhankarci.moviebackend.tvwatchlist

import com.emirhankarci.moviebackend.common.PageResponse
import com.emirhankarci.moviebackend.user.UserRepository
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tv/watchlist")
class TvWatchlistController(
    private val tvWatchlistService: TvWatchlistService,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvWatchlistController::class.java)
    }

    @PostMapping
    fun toggleWatchlist(@Valid @RequestBody request: TvWatchlistRequest): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("POST /api/tv/watchlist - toggle seriesId: {}", request.seriesId)

        return when (val result = tvWatchlistService.toggleWatchlist(user, request)) {
            is TvWatchlistResult.Success -> {
                val body = if (result.data is String) {
                    mapOf("message" to result.data)
                } else {
                    result.data
                }
                ResponseEntity.ok(body)
            }
            is TvWatchlistResult.Error -> ResponseEntity.badRequest().body(
                mapOf("error" to result.code, "message" to result.message)
            )
        }
    }

    @DeleteMapping("/{seriesId}")
    fun removeFromWatchlist(@PathVariable seriesId: Long): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("DELETE /api/tv/watchlist/{}", seriesId)

        return when (val result = tvWatchlistService.removeFromWatchlist(user, seriesId)) {
            is TvWatchlistResult.Success -> ResponseEntity.ok(mapOf("message" to result.data))
            is TvWatchlistResult.Error -> ResponseEntity.status(404).body(
                mapOf("error" to result.code, "message" to result.message) as Any
            )
        }
    }

    @GetMapping
    fun getWatchlist(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,

        @RequestParam(defaultValue = "false") paginated: Boolean,
        @RequestParam(defaultValue = "desc") sortOrder: String
    ): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/watchlist - page: {}, size: {}, paginated: {}", page, size, paginated)

        return when (val result = tvWatchlistService.getWatchlist(user, page, size, paginated, sortOrder)) {
            is TvWatchlistResult.Success -> ResponseEntity.ok(result.data)
            is TvWatchlistResult.Error -> ResponseEntity.status(400).body(
                mapOf("error" to result.code, "message" to result.message) as Any
            )
        }
    }

    @GetMapping("/status/{seriesId}")
    fun checkStatus(@PathVariable seriesId: Long): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/watchlist/status/{}", seriesId)

        val status = tvWatchlistService.checkStatus(user, seriesId)
        return ResponseEntity.ok(status)
    }

    private fun getCurrentUser() = SecurityContextHolder.getContext().authentication?.name
        ?.let { userRepository.findByUsername(it) }

    private fun unauthorized(): ResponseEntity<Any> = ResponseEntity.status(401).body(
        mapOf("error" to "UNAUTHORIZED", "message" to "Yetkilendirme gerekli") as Any
    )
}
