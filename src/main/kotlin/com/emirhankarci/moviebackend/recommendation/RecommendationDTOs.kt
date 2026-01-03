package com.emirhankarci.moviebackend.recommendation

/**
 * Individual recommended movie for display
 */
data class RecommendedMovie(
    val id: Long,
    val title: String,
    val posterPath: String?,
    val rating: Double,
    val releaseYear: Int,
    val genres: List<String>
)

/**
 * Response wrapper for recommendations endpoint
 */
data class RecommendationsResponse(
    val sourceMovieTitle: String,
    val displayMessage: String,
    val recommendations: List<RecommendedMovie>
)

/**
 * Empty response when user has no watched movies
 */
data class EmptyRecommendationsResponse(
    val message: String,
    val recommendations: List<RecommendedMovie> = emptyList()
)

/**
 * Result sealed class for service layer
 */
sealed class RecommendationResult {
    data class Success(val response: RecommendationsResponse) : RecommendationResult()
    data class Empty(val message: String) : RecommendationResult()
    data class Error(val code: String, val message: String) : RecommendationResult()
}

/**
 * Error response for API errors
 */
data class RecommendationErrorResponse(
    val error: String,
    val message: String
)


/**
 * Response for collection-based recommendations
 */
data class CollectionRecommendationsResponse(
    val collectionId: Long,
    val collectionName: String,
    val displayMessage: String,
    val recommendations: List<RecommendedMovie>
)

/**
 * Empty response for collection-based recommendations
 */
data class EmptyCollectionRecommendationsResponse(
    val message: String,
    val recommendations: List<RecommendedMovie> = emptyList()
)

/**
 * Result sealed class for collection-based recommendations
 */
sealed class CollectionRecommendationResult {
    data class Success(val response: CollectionRecommendationsResponse) : CollectionRecommendationResult()
    data class Empty(val message: String) : CollectionRecommendationResult()
    data class Error(val code: String, val message: String) : CollectionRecommendationResult()
}
