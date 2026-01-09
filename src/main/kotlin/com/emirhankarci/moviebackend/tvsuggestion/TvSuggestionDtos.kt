package com.emirhankarci.moviebackend.tvsuggestion

import java.time.LocalDate

// Response DTOs
data class TvSuggestion(
    val id: Long,
    val name: String,
    val posterPath: String?,
    val rating: Double,
    val voteCount: Int
)

data class DailyTvSuggestionsResponse(
    val suggestions: List<TvSuggestion>,
    val cached: Boolean,
    val generatedAt: LocalDate,
    val metadata: TvSuggestionMetadata? = null
)

/**
 * Metadata about how TV suggestions were generated.
 */
data class TvSuggestionMetadata(
    val personalizationTier: String,
    val dataSources: List<String>,
    val profileSummary: TvProfileSummary?
)

/**
 * Summary of user's TV profile data used for personalization.
 */
data class TvProfileSummary(
    val watchedCount: Int,
    val watchlistCount: Int,
    val hasPreferences: Boolean,
    val topGenres: List<String>?
)

// Result sealed class for service layer
sealed class TvSuggestionResult<out T> {
    data class Success<T>(val data: T) : TvSuggestionResult<T>()
    data class Error(val message: String, val code: TvSuggestionErrorCode = TvSuggestionErrorCode.UNKNOWN) : TvSuggestionResult<Nothing>()
}

enum class TvSuggestionErrorCode {
    USER_NOT_FOUND,
    AI_ERROR,
    TMDB_ERROR,
    VALIDATION_ERROR,
    UNKNOWN
}
