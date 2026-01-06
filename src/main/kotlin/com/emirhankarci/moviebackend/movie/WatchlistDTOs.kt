package com.emirhankarci.moviebackend.movie

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class WatchlistRequest(
    @field:Positive(message = "Movie ID must be greater than 0")
    val movieId: Long,
    
    @field:NotBlank(message = "Movie title cannot be empty")
    val movieTitle: String,
    
    val posterPath: String?,
    
    @field:DecimalMin(value = "0.0", message = "IMDb rating must be at least 0.0")
    @field:DecimalMax(value = "10.0", message = "IMDb rating must be at most 10.0")
    val imdbRating: Double? = null
)

data class WatchlistResponse(
    val id: Long,
    val movieId: Long,
    val movieTitle: String,
    val posterPath: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val addedAt: LocalDateTime,
    val imdbRating: Double? = null
)

data class WatchlistStatusResponse(
    val movieId: Long,
    val inWatchlist: Boolean
)

sealed class WatchlistResult<out T> {
    data class Success<T>(val data: T) : WatchlistResult<T>()
    data class Error(val message: String) : WatchlistResult<Nothing>()
}
