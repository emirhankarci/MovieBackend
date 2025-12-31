package com.emirhankarci.moviebackend.movie

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class WatchlistRequest(
    val movieId: Long,
    val movieTitle: String,
    val posterPath: String?,
    val imdbRating: Double? = null
) {
    fun validate(): ValidationResult {
        if (movieId <= 0) {
            return ValidationResult.Invalid("Movie ID must be greater than 0")
        }
        if (movieTitle.isBlank()) {
            return ValidationResult.Invalid("Movie title cannot be empty")
        }
        if (imdbRating != null && (imdbRating < 0.0 || imdbRating > 10.0)) {
            return ValidationResult.Invalid("IMDb rating must be between 0.0 and 10.0")
        }
        return ValidationResult.Valid
    }
}

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

data class PaginatedWatchlistResponse(
    val content: List<WatchlistResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

sealed class WatchlistResult<out T> {
    data class Success<T>(val data: T) : WatchlistResult<T>()
    data class Error(val message: String) : WatchlistResult<Nothing>()
}

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}
