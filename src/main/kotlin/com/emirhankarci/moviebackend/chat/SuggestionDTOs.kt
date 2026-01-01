package com.emirhankarci.moviebackend.chat

enum class SuggestionType {
    SIMILAR,    // Son izlenen filmlere benzer
    GENRE,      // Tercih edilen türlere göre
    TRENDING,   // Popüler/gündem
    SEASONAL    // Mevsimsel öneriler
}

data class SuggestionChip(
    val text: String,
    val emoji: String,
    val type: SuggestionType
)

data class SuggestionsResponse(
    val suggestions: List<SuggestionChip>
)

// Result type for suggestions
sealed class SuggestionResult<out T> {
    data class Success<T>(val data: T) : SuggestionResult<T>()
    data class Error(val message: String, val code: SuggestionErrorCode) : SuggestionResult<Nothing>()
}

enum class SuggestionErrorCode {
    USER_NOT_FOUND,
    INTERNAL_ERROR
}
