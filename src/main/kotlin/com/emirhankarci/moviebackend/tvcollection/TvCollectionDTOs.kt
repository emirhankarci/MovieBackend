package com.emirhankarci.moviebackend.tvcollection

import com.emirhankarci.moviebackend.common.PageResponse
import java.time.LocalDateTime

// Request DTOs
data class CreateTvCollectionRequest(
    val name: String,
    val description: String? = null
)

data class UpdateTvCollectionRequest(
    val name: String? = null,
    val description: String? = null
)

data class AddSeriesToCollectionRequest(
    val seriesId: Long,
    val seriesName: String,
    val posterPath: String? = null
)

// Response DTOs
data class TvCollectionSummaryResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val coverPosters: List<String?>,
    val seriesCount: Int,
    val createdAt: LocalDateTime
)

data class TvCollectionDetailResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val coverPosters: List<String?>,
    val seriesCount: Int,
    val createdAt: LocalDateTime,
    val series: PageResponse<TvCollectionSeriesDto>
)

data class TvCollectionSeriesDto(
    val id: Long,
    val seriesId: Long,
    val seriesName: String,
    val posterPath: String?,
    val addedAt: LocalDateTime
)

data class SeriesCollectionStatusResponse(
    val seriesId: Long,
    val collections: List<CollectionInfoDto>
)

data class CollectionInfoDto(
    val id: Long,
    val name: String
)

// Result wrapper
sealed class TvCollectionResult<out T> {
    data class Success<T>(val data: T) : TvCollectionResult<T>()
    data class Error(val code: String, val message: String) : TvCollectionResult<Nothing>()
}
