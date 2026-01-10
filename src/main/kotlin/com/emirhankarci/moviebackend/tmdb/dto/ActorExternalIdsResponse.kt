package com.emirhankarci.moviebackend.tmdb.dto

/**
 * Actor External IDs API Response DTO
 */
data class ActorExternalIdsResponse(
    val actorId: Long,
    val instagramId: String?,
    val twitterId: String?,
    val instagramUrl: String?,
    val twitterUrl: String?
)
