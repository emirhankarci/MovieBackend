package com.emirhankarci.moviebackend.tvseries.dto

import com.emirhankarci.moviebackend.tmdb.dto.GenreDto

/**
 * TV Series Response DTOs
 * API response'larında kullanılan veri transfer objeleri.
 */

// ==================== TV Series Detail ====================

data class TvSeriesDetailResponse(
    val id: Long,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val firstAirDate: String?,
    val lastAirDate: String?,
    val status: String,
    val tagline: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val numberOfSeasons: Int,
    val numberOfEpisodes: Int,
    val episodeRunTime: List<Int>,
    val genres: List<GenreDto>,
    val networks: List<NetworkDto>,
    val seasons: List<SeasonSummaryDto>
)

data class NetworkDto(
    val id: Int,
    val name: String,
    val logoPath: String?
)

data class SeasonSummaryDto(
    val id: Long,
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val airDate: String?,
    val posterPath: String?
)

// ==================== Season Detail ====================

data class SeasonDetailResponse(
    val id: Long,
    val seriesId: Long,
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val airDate: String?,
    val posterPath: String?,
    val episodes: List<EpisodeSummaryDto>
)


data class EpisodeSummaryDto(
    val id: Long,
    val episodeNumber: Int,
    val name: String,
    val overview: String?,
    val airDate: String?,
    val stillPath: String?,
    val voteAverage: Double,
    val runtime: Int?
)

// ==================== Episode Detail ====================

data class EpisodeDetailResponse(
    val id: Long,
    val seriesId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String?,
    val airDate: String?,
    val stillPath: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val runtime: Int?,
    val guestStars: List<GuestStarDto>
)

data class GuestStarDto(
    val id: Long,
    val name: String,
    val character: String,
    val profilePath: String?
)

// ==================== TV Series Credits ====================

data class TvSeriesCreditsResponse(
    val seriesId: Long,
    val cast: List<TvCastMemberDto>,
    val crew: List<TvCrewMemberDto>
)

data class TvCastMemberDto(
    val id: Long,
    val name: String,
    val character: String,
    val profilePath: String?,
    val order: Int
)

data class TvCrewMemberDto(
    val id: Long,
    val name: String,
    val job: String,
    val department: String,
    val profilePath: String?
)

// ==================== Error Response ====================

data class TvSeriesErrorResponse(
    val error: String,
    val message: String
)
