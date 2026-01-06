package com.emirhankarci.moviebackend.usercollection

import com.emirhankarci.moviebackend.common.PageResponse
import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// Request DTOs
data class CreateCollectionRequest(
    @field:NotBlank(message = "Collection name cannot be empty")
    @field:Size(max = 255, message = "Collection name cannot exceed 255 characters")
    val name: String,
    
    val description: String? = null
)

data class UpdateCollectionRequest(
    @field:Size(max = 255, message = "Collection name cannot exceed 255 characters")
    val name: String? = null,
    
    val description: String? = null
)

data class AddMovieRequest(
    @field:Positive(message = "Movie ID must be greater than 0")
    val movieId: Long,
    
    @field:NotBlank(message = "Movie title cannot be empty")
    val movieTitle: String,
    
    val posterPath: String? = null
)

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

// Error response
data class UserCollectionErrorResponse(
    val error: String,
    val message: String
)
