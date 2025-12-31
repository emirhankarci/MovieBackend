package com.emirhankarci.moviebackend.watched

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class WatchedMovieRequest(
    val movieId: Long,
    val movieTitle: String,
    val posterPath: String?,
    val imdbRating: Double? = null
) {
    fun validate(): WatchedMovieValidationResult {
        if (movieId <= 0) {
            return WatchedMovieValidationResult.Invalid("Movie ID must be greater than 0")
        }
        if (movieTitle.isBlank()) {
            return WatchedMovieValidationResult.Invalid("Movie title cannot be empty")
        }
        if (imdbRating != null && (imdbRating < 0.0 || imdbRating > 10.0)) {
            return WatchedMovieValidationResult.Invalid("IMDb rating must be between 0.0 and 10.0")
        }
        return WatchedMovieValidationResult.Valid
    }
}

data class RateMovieRequest(
    val movieId: Long,
    val movieTitle: String,
    val posterPath: String?,
    val rating: Double,
    val imdbRating: Double? = null
) {
    fun validate(): RatingValidationResult {
        if (movieId <= 0) {
            return RatingValidationResult.Invalid("Movie ID must be greater than 0")
        }
        if (movieTitle.isBlank()) {
            return RatingValidationResult.Invalid("Movie title cannot be empty")
        }
        if (rating < 1.0 || rating > 10.0) {
            return RatingValidationResult.Invalid("Rating must be between 1.0 and 10.0")
        }
        // 0.5 increment check: rating * 2 must be a whole number
        if ((rating * 2) != (rating * 2).toLong().toDouble()) {
            return RatingValidationResult.Invalid("Rating must be in 0.5 increments (e.g., 7.0, 7.5, 8.0)")
        }
        if (imdbRating != null && (imdbRating < 0.0 || imdbRating > 10.0)) {
            return RatingValidationResult.Invalid("IMDb rating must be between 0.0 and 10.0")
        }
        return RatingValidationResult.Valid
    }
}

sealed class RatingValidationResult {
    data object Valid : RatingValidationResult()
    data class Invalid(val message: String) : RatingValidationResult()
}

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

sealed class WatchedMovieValidationResult {
    data object Valid : WatchedMovieValidationResult()
    data class Invalid(val message: String) : WatchedMovieValidationResult()
}
