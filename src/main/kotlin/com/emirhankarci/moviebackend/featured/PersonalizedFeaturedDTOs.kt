package com.emirhankarci.moviebackend.featured

import com.emirhankarci.moviebackend.suggestion.ProfileSummary
import java.time.LocalDate

/**
 * Personalized featured movie with hook message for carousel display
 */
data class PersonalizedFeaturedMovie(
    val id: Long = 0,
    val title: String = "",
    val backdropPath: String = "",
    val tagline: String = "",
    val rating: Double = 0.0,
    val releaseYear: Int = 0,
    val genres: List<String> = emptyList(),
    val hookMessage: String = ""
)

/**
 * Response wrapper for personalized featured movies endpoint
 */
data class PersonalizedFeaturedResponse(
    val movies: List<PersonalizedFeaturedMovie> = emptyList(),
    val cached: Boolean = false,
    val generatedAt: String = LocalDate.now().toString(),
    val metadata: PersonalizationMetadata? = null
)

/**
 * Metadata about how personalization was generated
 */
data class PersonalizationMetadata(
    val personalizationTier: String = "NONE",
    val dataSources: List<String> = emptyList(),
    val profileSummary: ProfileSummary? = null
)

/**
 * Hook message for a specific movie
 */
data class HookMessage(
    val movieId: Long,
    val message: String
)

/**
 * Result sealed class for personalized featured service layer
 */
sealed class PersonalizedFeaturedResult {
    data class Success(val data: PersonalizedFeaturedResponse) : PersonalizedFeaturedResult()
    data class Error(val code: String, val message: String) : PersonalizedFeaturedResult()
}

/**
 * Cache data wrapper for personalized featured movies
 */
data class PersonalizedFeaturedCacheData(
    val movies: List<PersonalizedFeaturedMovie> = emptyList(),
    val metadata: PersonalizationMetadata? = null,
    val generatedAt: String = LocalDate.now().toString()
)
