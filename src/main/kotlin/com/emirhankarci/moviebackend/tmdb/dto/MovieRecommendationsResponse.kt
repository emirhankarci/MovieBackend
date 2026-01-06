package com.emirhankarci.moviebackend.tmdb.dto

/**
 * Movie Recommendations API Response DTOs
 */

data class RecommendedMovieDto(
    val id: Long,
    val title: String,
    val posterPath: String?,
    val voteAverage: Double
)

data class MovieRecommendationsResponse(
    val movieId: Long,
    val recommendations: List<RecommendedMovieDto>
)
