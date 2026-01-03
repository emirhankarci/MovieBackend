package com.emirhankarci.moviebackend.collection

/**
 * Individual movie DTO for collection responses
 */
data class CollectionMovieDto(
    val id: Long,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val releaseYear: Int
)

/**
 * Main response for prerequisites endpoint
 */
data class PrerequisiteMoviesResponse(
    val collectionId: Long,
    val collectionName: String,
    val totalMoviesInCollection: Int,
    val targetMovie: CollectionMovieDto,
    val displayMessage: String,
    val prerequisites: List<CollectionMovieDto>
)

/**
 * Response when movie doesn't belong to any collection
 */
data class NoCollectionResponse(
    val message: String,
    val hasCollection: Boolean = false
)

/**
 * Response when movie is first in series (no prerequisites)
 */
data class NoPrerequisitesResponse(
    val collectionId: Long,
    val collectionName: String,
    val message: String,
    val hasPrerequisites: Boolean = false
)

/**
 * Result sealed class for service layer
 */
sealed class CollectionResult {
    data class Success(val response: PrerequisiteMoviesResponse) : CollectionResult()
    data class NoCollection(val message: String) : CollectionResult()
    data class NoPrerequisites(val collectionId: Long, val collectionName: String, val message: String) : CollectionResult()
    data class Error(val code: String, val message: String) : CollectionResult()
}

/**
 * Error response for API errors
 */
data class CollectionErrorResponse(
    val error: String,
    val message: String
)
