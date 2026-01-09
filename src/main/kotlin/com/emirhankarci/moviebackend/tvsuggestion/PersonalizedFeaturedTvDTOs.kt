package com.emirhankarci.moviebackend.tvsuggestion

import java.time.LocalDate

/**
 * Personalized featured TV series with hook message for carousel display
 */
data class PersonalizedFeaturedTvSeries(
    val id: Long = 0,
    val name: String = "",
    val backdropPath: String = "",
    val tagline: String = "",
    val rating: Double = 0.0,
    val firstAirYear: Int = 0,
    val genres: List<String> = emptyList(),
    val hookMessage: String = ""
)

/**
 * Response wrapper for personalized featured TV series endpoint
 */
data class PersonalizedFeaturedTvResponse(
    val tvSeries: List<PersonalizedFeaturedTvSeries> = emptyList(),
    val cached: Boolean = false,
    val generatedAt: String = LocalDate.now().toString(),
    val metadata: TvPersonalizationMetadata? = null
)

/**
 * Metadata about how TV personalization was generated
 */
data class TvPersonalizationMetadata(
    val personalizationTier: String = "NONE",
    val dataSources: List<String> = emptyList(),
    val profileSummary: TvProfileSummary? = null
)

/**
 * Result sealed class for personalized featured TV service layer
 */
sealed class PersonalizedFeaturedTvResult {
    data class Success(val data: PersonalizedFeaturedTvResponse) : PersonalizedFeaturedTvResult()
    data class Error(val code: String, val message: String) : PersonalizedFeaturedTvResult()
}

/**
 * Cache data wrapper for personalized featured TV series
 */
data class PersonalizedFeaturedTvCacheData(
    val tvSeries: List<PersonalizedFeaturedTvSeries> = emptyList(),
    val metadata: TvPersonalizationMetadata? = null,
    val generatedAt: String = LocalDate.now().toString()
)
