package com.emirhankarci.moviebackend.tmdb.dto

/**
 * Actor TV Credits API Response DTO
 */
data class ActorTvCreditsResponse(
    val actorId: Long,
    val actorName: String,
    val tvShows: List<ActorTvCreditDto>
)

data class ActorTvCreditDto(
    val id: Long,
    val name: String,
    val character: String?,
    val posterPath: String?,
    val firstAirDate: String?,
    val voteAverage: Double,
    val episodeCount: Int?
)

/**
 * Paginated Actor TV Credits Response
 */
data class PaginatedActorTvCreditsResponse(
    val actorId: Long,
    val actorName: String,
    val tvShows: List<ActorTvCreditDto>,
    val pagination: PaginationInfo
)
