package com.emirhankarci.moviebackend.tvwatchlist

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class TvWatchlistRequest(
    @field:Positive(message = "Series ID must be greater than 0")
    val seriesId: Long,
    
    @field:NotBlank(message = "Series name cannot be empty")
    val seriesName: String,
    
    val posterPath: String?,
    
    @field:DecimalMin(value = "0.0", message = "Vote average must be at least 0.0")
    @field:DecimalMax(value = "10.0", message = "Vote average must be at most 10.0")
    val voteAverage: Double? = null
)

data class TvWatchlistResponse(
    val id: Long,
    val seriesId: Long,
    val seriesName: String,
    val posterPath: String?,
    val voteAverage: Double?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val addedAt: LocalDateTime
)

data class TvWatchlistStatusResponse(
    val seriesId: Long,
    val inWatchlist: Boolean
)

data class TvWatchlistErrorResponse(
    val error: String,
    val message: String
)

sealed class TvWatchlistResult<out T> {
    data class Success<T>(val data: T) : TvWatchlistResult<T>()
    data class Error(val code: String, val message: String) : TvWatchlistResult<Nothing>()
}
