package com.emirhankarci.moviebackend.watched

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class WatchedMovieRequest(
    @field:Positive(message = "Movie ID must be greater than 0")
    val movieId: Long,
    
    @field:NotBlank(message = "Movie title cannot be empty")
    val movieTitle: String,
    
    val posterPath: String?,
    
    @field:DecimalMin(value = "0.0", message = "IMDb rating must be at least 0.0")
    @field:DecimalMax(value = "10.0", message = "IMDb rating must be at most 10.0")
    val imdbRating: Double? = null
)

data class RateMovieRequest(
    @field:Positive(message = "Movie ID must be greater than 0")
    val movieId: Long,
    
    @field:NotBlank(message = "Movie title cannot be empty")
    val movieTitle: String,
    
    val posterPath: String?,
    
    @field:DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @field:DecimalMax(value = "10.0", message = "Rating must be at most 10.0")
    val rating: Double,
    
    @field:DecimalMin(value = "0.0", message = "IMDb rating must be at least 0.0")
    @field:DecimalMax(value = "10.0", message = "IMDb rating must be at most 10.0")
    val imdbRating: Double? = null
)

data class RateMovieResponse(
    val movieId: Long,
    val rating: Double,
    val isNewlyWatched: Boolean,
    val message: String
)

data class WatchedMovieResponse(
    val id: Long,
    val movieId: Long,
    val movieTitle: String,
    val posterPath: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val watchedAt: LocalDateTime,
    val userRating: Double?,
    val imdbRating: Double? = null
)

data class WatchedMovieStatusResponse(
    val movieId: Long,
    val isWatched: Boolean,
    val userRating: Double? = null
)

sealed class WatchedMovieResult<out T> {
    data class Success<T>(val data: T) : WatchedMovieResult<T>()
    data class Error(val message: String) : WatchedMovieResult<Nothing>()
}
