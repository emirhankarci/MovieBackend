package com.emirhankarci.moviebackend.featured

/**
 * Featured movie data for carousel display
 */
data class FeaturedMovie(
    val id: Long,
    val title: String,
    val backdropPath: String,
    val tagline: String,
    val rating: Double,
    val releaseYear: Int,
    val genres: List<String>
)

/**
 * Response wrapper for featured movies endpoint
 */
data class FeaturedMoviesResponse(
    val movies: List<FeaturedMovie>
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
