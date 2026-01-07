package com.emirhankarci.moviebackend.watchedepisode

import java.time.LocalDateTime

// Request DTOs
data class MarkEpisodeWatchedRequest(
    val seriesId: Long,
    val seriesName: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeName: String? = null
)

data class UnmarkEpisodeRequest(
    val seriesId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int
)

data class MarkSeasonWatchedRequest(
    val seriesId: Long,
    val seriesName: String,
    val seasonNumber: Int,
    val episodes: List<EpisodeInfo>
)

data class UnmarkSeasonRequest(
    val seriesId: Long,
    val seasonNumber: Int
)

data class EpisodeInfo(
    val episodeNumber: Int,
    val episodeName: String? = null
)

// Response DTOs
data class WatchedEpisodesResponse(
    val seriesId: Long,
    val seriesName: String,
    val seasons: List<WatchedSeasonDto>
)

data class WatchedSeasonDto(
    val seasonNumber: Int,
    val episodes: List<WatchedEpisodeDto>
)

data class WatchedEpisodeDto(
    val episodeNumber: Int,
    val episodeName: String?,
    val watchedAt: LocalDateTime
)

data class WatchProgressResponse(
    val seriesId: Long,
    val totalEpisodes: Int,
    val watchedEpisodes: Int,
    val progressPercentage: Double,
    val seasonProgress: List<SeasonProgressDto>
)

data class SeasonProgressDto(
    val seasonNumber: Int,
    val totalEpisodes: Int,
    val watchedEpisodes: Int,
    val progressPercentage: Double
)

data class EpisodeWatchStatusResponse(
    val seriesId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val isWatched: Boolean
)

data class WatchedSeriesSummary(
    val seriesId: Long,
    val seriesName: String,
    val lastWatchedAt: LocalDateTime
)

data class WatchedSeriesDto(
    val seriesId: Long,
    val seriesName: String,
    val lastWatchedAt: LocalDateTime,
    val posterPath: String? = null,
    val backdropPath: String? = null
)

// Result wrapper
sealed class WatchedEpisodeResult<out T> {
    data class Success<T>(val data: T) : WatchedEpisodeResult<T>()
    data class Error(val code: String, val message: String) : WatchedEpisodeResult<Nothing>()
}
