package com.emirhankarci.moviebackend.watched

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class WatchedMovieRequest(
    val movieId: Long,
    val movieTitle: String,
    val posterPath: String?
) {
    fun validate(): WatchedMovieValidationResult {
        if (movieId <= 0) {
            return WatchedMovieValidationResult.Invalid("Movie ID must be greater than 0")
        }
        if (movieTitle.isBlank()) {
            return WatchedMovieValidationResult.Invalid("Movie title cannot be empty")
        }
        return WatchedMovieValidationResult.Valid
    }
}

data class WatchedMovieResponse(
    val id: Long,
    val movieId: Long,
    val movieTitle: String,
    val posterPath: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val watchedAt: LocalDateTime,
    val userRating: Int?
)

data class WatchedMovieStatusResponse(
    val movieId: Long,
    val isWatched: Boolean
)

sealed class WatchedMovieResult<out T> {
    data class Success<T>(val data: T) : WatchedMovieResult<T>()
    data class Error(val message: String) : WatchedMovieResult<Nothing>()
}

sealed class WatchedMovieValidationResult {
    data object Valid : WatchedMovieValidationResult()
    data class Invalid(val message: String) : WatchedMovieValidationResult()
}
