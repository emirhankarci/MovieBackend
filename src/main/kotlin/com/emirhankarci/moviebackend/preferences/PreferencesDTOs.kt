package com.emirhankarci.moviebackend.preferences

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class SavePreferencesRequest(
    val genres: List<String>,
    val preferredEra: String,
    val moods: List<String>,
    val favoriteMovieIds: List<Long>
) {
    fun validate(): PreferencesValidationResult {
        // Check minimum 3 genres
        if (genres.size < 3) {
            return PreferencesValidationResult.Invalid("At least 3 genres must be selected")
        }

        // Check minimum 3 favorite movies
        if (favoriteMovieIds.size < 3) {
            return PreferencesValidationResult.Invalid("At least 3 favorite movies must be selected")
        }

        // Validate each genre against enum
        for (genre in genres) {
            try {
                Genre.valueOf(genre.uppercase())
            } catch (e: IllegalArgumentException) {
                val validGenres = Genre.entries.joinToString(", ") { it.name }
                return PreferencesValidationResult.Invalid(
                    "Invalid genre: $genre. Valid options: $validGenres"
                )
            }
        }

        // Validate era against enum
        try {
            Era.valueOf(preferredEra.uppercase())
        } catch (e: IllegalArgumentException) {
            val validEras = Era.entries.joinToString(", ") { it.name }
            return PreferencesValidationResult.Invalid(
                "Invalid era: $preferredEra. Valid options: $validEras"
            )
        }

        // Validate each mood against enum
        for (mood in moods) {
            try {
                Mood.valueOf(mood.uppercase())
            } catch (e: IllegalArgumentException) {
                val validMoods = Mood.entries.joinToString(", ") { it.name }
                return PreferencesValidationResult.Invalid(
                    "Invalid mood: $mood. Valid options: $validMoods"
                )
            }
        }

        // Validate movie IDs are positive
        for (movieId in favoriteMovieIds) {
            if (movieId <= 0) {
                return PreferencesValidationResult.Invalid("Movie ID must be greater than 0")
            }
        }

        return PreferencesValidationResult.Valid
    }
}

data class PreferencesResponse(
    val genres: List<String>,
    val preferredEra: String,
    val moods: List<String>,
    val favoriteMovieIds: List<Long>,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val updatedAt: LocalDateTime
)

data class PreferencesStatusResponse(
    val hasPreferences: Boolean
)

data class CuratedFilm(
    val tmdbId: Long,
    val title: String,
    val posterPath: String
)

sealed class PreferencesResult<out T> {
    data class Success<T>(val data: T) : PreferencesResult<T>()
    data class Error(val message: String) : PreferencesResult<Nothing>()
}

sealed class PreferencesValidationResult {
    data object Valid : PreferencesValidationResult()
    data class Invalid(val message: String) : PreferencesValidationResult()
}
