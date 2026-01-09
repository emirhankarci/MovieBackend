package com.emirhankarci.moviebackend.featured

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/featured")
class FeaturedController(
    private val featuredMoviesService: FeaturedMoviesService,
    private val featuredTvSeriesService: FeaturedTvSeriesService,
    private val personalizedFeaturedService: PersonalizedFeaturedService
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

    @GetMapping("/tv-series")
    fun getFeaturedTvSeries(
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
        
        return when (val result = featuredTvSeriesService.getFeaturedTvSeries(window, limit)) {
            is FeaturedTvSeriesResult.Success -> {
                ResponseEntity.ok(
                    FeaturedTvSeriesResponse(tvSeries = result.tvSeries)
                )
            }
            is FeaturedTvSeriesResult.Error -> {
                ResponseEntity.status(503).body(
                    FeaturedErrorResponse(
                        error = result.code,
                        message = result.message
                    )
                )
            }
        }
    }

    @GetMapping("/movies/personalized")
    fun getPersonalizedFeaturedMovies(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(
                FeaturedErrorResponse(
                    error = "UNAUTHORIZED",
                    message = "Bu endpoint için giriş yapmanız gerekiyor"
                )
            )
        
        return try {
            when (val result = personalizedFeaturedService.getPersonalizedFeaturedMovies(username)) {
                is PersonalizedFeaturedResult.Success -> {
                    ResponseEntity.ok(result.data)
                }
                is PersonalizedFeaturedResult.Error -> {
                    val status = when (result.code) {
                        "USER_NOT_FOUND" -> 404
                        "TRENDING_ERROR" -> 503
                        else -> 500
                    }
                    ResponseEntity.status(status).body(
                        FeaturedErrorResponse(
                            error = result.code,
                            message = result.message
                        )
                    )
                }
            }
        } catch (e: Exception) {
            ResponseEntity.status(500).body(
                FeaturedErrorResponse(
                    error = "INTERNAL_ERROR",
                    message = "Beklenmeyen hata: ${e.message}"
                )
            )
        }
    }
}
