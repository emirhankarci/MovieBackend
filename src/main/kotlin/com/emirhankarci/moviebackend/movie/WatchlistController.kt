package com.emirhankarci.moviebackend.movie

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/movies/watchlist")
class WatchlistController(
    private val watchlistService: WatchlistService
) {

    @PostMapping
    fun toggleWatchlist(@RequestBody request: WatchlistRequest): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))
        
        return when (val result = watchlistService.toggleWatchlist(username, request)) {
            is WatchlistResult.Success -> ResponseEntity.ok(mapOf("message" to result.data))
            is WatchlistResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }

    @GetMapping
    fun getWatchlist(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "false") paginated: Boolean
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))
        
        return when (val result = watchlistService.getUserWatchlist(username, page, size, paginated)) {
            is WatchlistResult.Success -> ResponseEntity.ok(result.data)
            is WatchlistResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }

    @GetMapping("/{movieId}/status")
    fun checkWatchlistStatus(@PathVariable movieId: Long): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))
        
        return when (val result = watchlistService.isMovieInWatchlist(username, movieId)) {
            is WatchlistResult.Success -> ResponseEntity.ok(result.data)
            is WatchlistResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }

    @DeleteMapping("/{movieId}")
    fun removeFromWatchlist(@PathVariable movieId: Long): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))
        
        return when (val result = watchlistService.removeFromWatchlist(username, movieId)) {
            is WatchlistResult.Success -> ResponseEntity.ok(mapOf("message" to result.data))
            is WatchlistResult.Error -> ResponseEntity.status(404).body(mapOf("message" to result.message))
        }
    }
}
