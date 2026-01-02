package com.emirhankarci.moviebackend.featured

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/featured")
class FeaturedController(
    private val featuredMoviesService: FeaturedMoviesService
) {

    @GetMapping("/movies")
    fun getFeaturedMovies(
        @RequestParam(defaultValue = "day") timeWindow: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<Any> {
        // Validate timeWindow parameter
        if (!TimeWindow.isValid(timeWindow)) {
            return ResponseEntity.badRequest().body(
                FeaturedErrorResponse(
                    error = "INVALID_TIME_WINDOW",
                    message = "Geçersiz zaman aralığı: $timeWindow. Geçerli değerler: day, week"
                )
            )
        }

        val window = TimeWindow.fromString(timeWindow)
        
        return when (val result = featuredMoviesService.getFeaturedMovies(window, limit)) {
            is FeaturedMoviesResult.Success -> {
                ResponseEntity.ok(
                    FeaturedMoviesResponse(movies = result.movies)
                )
            }
            is FeaturedMoviesResult.Error -> {
                ResponseEntity.status(503).body(
                    FeaturedErrorResponse(
                        error = result.code,
                        message = result.message
                    )
                )
            }
        }
    }
}
