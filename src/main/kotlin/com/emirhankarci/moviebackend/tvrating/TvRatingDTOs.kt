package com.emirhankarci.moviebackend.tvrating

import java.time.LocalDateTime

// Series Rating DTOs
data class RateSeriesRequest(
    val seriesId: Long,
    val seriesName: String,
    val posterPath: String? = null,
    val rating: Double  // 1.0 - 10.0
)

data class RatedSeriesResponse(
    val id: Long,
    val seriesId: Long,
    val seriesName: String,
    val posterPath: String?,
    val rating: Double,
    val ratedAt: LocalDateTime
)

data class SeriesRatingStatusResponse(
    val seriesId: Long,
    val isRated: Boolean,
    val rating: Double?
)

// Episode Rating DTOs
data class RateEpisodeRequest(
    val seriesId: Long,
    val seriesName: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeName: String? = null,
    val rating: Double  // 1.0 - 10.0
)

data class DeleteEpisodeRatingRequest(
    val seriesId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int
)

data class RatedEpisodeResponse(
    val id: Long,
    val seriesId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeName: String?,
    val rating: Double,
    val ratedAt: LocalDateTime
)

data class RatedEpisodesResponse(
    val seriesId: Long,
    val episodes: List<RatedEpisodeResponse>
)

// Result wrapper
sealed class TvRatingResult<out T> {
    data class Success<T>(val data: T) : TvRatingResult<T>()
    data class Error(val code: String, val message: String) : TvRatingResult<Nothing>()
}
