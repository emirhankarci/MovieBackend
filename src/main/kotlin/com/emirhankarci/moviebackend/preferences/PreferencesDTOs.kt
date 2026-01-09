package com.emirhankarci.moviebackend.preferences

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class SavePreferencesRequest(
    @field:NotEmpty(message = "At least one genre must be selected")
    @field:Size(min = 3, message = "At least 3 genres must be selected")
    val genres: List<String>,
    
    @field:NotBlank(message = "Preferred era cannot be empty")
    val preferredEra: String,
    
    @field:NotEmpty(message = "At least one mood must be selected")
    val moods: List<String>,
    
    @field:NotEmpty(message = "At least one favorite movie must be selected")
    @field:Size(min = 3, message = "At least 3 favorite movies must be selected")
    val favoriteMovieIds: List<@Positive(message = "Movie ID must be greater than 0") Long>,
    
    @field:Size(min = 3, message = "At least 3 favorite TV series must be selected")
    val favoriteTvSeriesIds: List<@Positive(message = "TV Series ID must be greater than 0") Long>? = null
) {
    // Enum validation stays in service layer (would need custom validators)
    fun validateEnums(): PreferencesValidationResult {
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

        return PreferencesValidationResult.Valid
    }
}

data class PreferencesResponse(
    val genres: List<String>,
    val preferredEra: String,
    val moods: List<String>,
    val favoriteMovieIds: List<Long>,
    val favoriteTvSeriesIds: List<Long>? = null,
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

data class CuratedTvSeries(
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
