package com.emirhankarci.moviebackend.usercollection

import com.emirhankarci.moviebackend.common.PageResponse
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

// Request DTOs
data class CreateCollectionRequest(
    val name: String,
    val description: String? = null
) {
    fun validate(): UserCollectionValidationResult {
        if (name.isBlank()) {
            return UserCollectionValidationResult.Invalid("Collection name cannot be empty")
        }
        if (name.length > 255) {
            return UserCollectionValidationResult.Invalid("Collection name cannot exceed 255 characters")
        }
        return UserCollectionValidationResult.Valid
    }
}

data class UpdateCollectionRequest(
    val name: String? = null,
    val description: String? = null
) {
    fun validate(): UserCollectionValidationResult {
        if (name != null && name.isBlank()) {
            return UserCollectionValidationResult.Invalid("Collection name cannot be empty")
        }
        if (name != null && name.length > 255) {
            return UserCollectionValidationResult.Invalid("Collection name cannot exceed 255 characters")
        }
        return UserCollectionValidationResult.Valid
    }
}

data class AddMovieRequest(
    val movieId: Long,
    val movieTitle: String,
    val posterPath: String? = null
) {
    fun validate(): UserCollectionValidationResult {
        if (movieId <= 0) {
            return UserCollectionValidationResult.Invalid("Movie ID must be greater than 0")
        }
        if (movieTitle.isBlank()) {
            return UserCollectionValidationResult.Invalid("Movie title cannot be empty")
        }
        return UserCollectionValidationResult.Valid
    }
}

// Response DTOs
data class CollectionResponse(
    val id: Long,
    val name: String,
    val description: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val updatedAt: LocalDateTime
)

data class CollectionSummaryResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val coverPosters: List<String?>,
    val movieCount: Int,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime
)

data class CollectionDetailResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val coverPosters: List<String?>,
    val movieCount: Int,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,
    val movies: PageResponse<CollectionMovieResponse>
)

data class CollectionMovieResponse(
    val id: Long,
    val movieId: Long,
    val movieTitle: String,
    val posterPath: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val addedAt: LocalDateTime
)

data class MovieCollectionStatusResponse(
    val movieId: Long,
    val collections: List<CollectionInfo>
)

data class CollectionInfo(
    val id: Long,
    val name: String
)

// Result sealed class
sealed class UserCollectionResult<out T> {
    data class Success<T>(val data: T) : UserCollectionResult<T>()
    data class Error(val code: String, val message: String) : UserCollectionResult<Nothing>()
}

// Validation sealed class
sealed class UserCollectionValidationResult {
    data object Valid : UserCollectionValidationResult()
    data class Invalid(val message: String) : UserCollectionValidationResult()
}

// Error response
data class UserCollectionErrorResponse(
    val error: String,
    val message: String
)
