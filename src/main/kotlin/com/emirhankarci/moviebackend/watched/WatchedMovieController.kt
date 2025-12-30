package com.emirhankarci.moviebackend.watched

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/movies/watched")
class WatchedMovieController(
    private val watchedMovieService: WatchedMovieService
) {

    @PostMapping
    fun addWatchedMovie(@RequestBody request: WatchedMovieRequest): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = watchedMovieService.addWatchedMovie(username, request)) {
            is WatchedMovieResult.Success -> ResponseEntity.ok(mapOf("message" to result.data))
            is WatchedMovieResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }

    @DeleteMapping("/{movieId}")
    fun removeWatchedMovie(@PathVariable movieId: Long): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = watchedMovieService.removeWatchedMovie(username, movieId)) {
            is WatchedMovieResult.Success -> ResponseEntity.ok(mapOf("message" to result.data))
            is WatchedMovieResult.Error -> ResponseEntity.status(404).body(mapOf("message" to result.message))
        }
    }

    @GetMapping
    fun getWatchedMovies(
        @RequestParam(defaultValue = "desc") sortOrder: String
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = watchedMovieService.getUserWatchedMovies(username, sortOrder)) {
            is WatchedMovieResult.Success -> ResponseEntity.ok(result.data)
            is WatchedMovieResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }

    @GetMapping("/{movieId}/status")
    fun checkWatchedStatus(@PathVariable movieId: Long): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = watchedMovieService.isMovieWatched(username, movieId)) {
            is WatchedMovieResult.Success -> ResponseEntity.ok(result.data)
            is WatchedMovieResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }
}
