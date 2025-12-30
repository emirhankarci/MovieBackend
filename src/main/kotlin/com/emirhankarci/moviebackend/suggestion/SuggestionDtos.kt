package com.emirhankarci.moviebackend.suggestion

import java.time.LocalDate

// Response DTOs
data class MovieSuggestion(
    val id: Long,
    val title: String,
    val posterPath: String?,
    val rating: Double,
    val voteCount: Int
)

data class DailySuggestionsResponse(
    val suggestions: List<MovieSuggestion>,
    val cached: Boolean,
    val generatedAt: LocalDate
)

// Result sealed class for service layer
sealed class SuggestionResult<out T> {
    data class Success<T>(val data: T) : SuggestionResult<T>()
    data class Error(val message: String, val code: SuggestionErrorCode = SuggestionErrorCode.UNKNOWN) : SuggestionResult<Nothing>()
}

enum class SuggestionErrorCode {
    USER_NOT_FOUND,
    AI_ERROR,
    TMDB_ERROR,
    VALIDATION_ERROR,
    UNKNOWN
}
