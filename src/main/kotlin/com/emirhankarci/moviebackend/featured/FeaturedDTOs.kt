package com.emirhankarci.moviebackend.featured

/**
 * Featured movie data for carousel display
 */
data class FeaturedMovie(
    val id: Long = 0,
    val title: String = "",
    val backdropPath: String = "",
    val tagline: String = "",
    val rating: Double = 0.0,
    val releaseYear: Int = 0,
    val genres: List<String> = emptyList()
)

/**
 * Response wrapper for featured movies endpoint
 */
data class FeaturedMoviesResponse(
    val movies: List<FeaturedMovie> = emptyList()
)

/**
 * Result sealed class for service layer
 */
sealed class FeaturedMoviesResult {
    data class Success(val movies: List<FeaturedMovie>) : FeaturedMoviesResult()
    data class Error(val code: String, val message: String) : FeaturedMoviesResult()
}

/**
 * Error response for API errors
 */
data class FeaturedErrorResponse(
    val error: String,
    val message: String
)
