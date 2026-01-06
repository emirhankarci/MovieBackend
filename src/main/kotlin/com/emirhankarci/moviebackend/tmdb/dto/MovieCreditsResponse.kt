package com.emirhankarci.moviebackend.tmdb.dto

/**
 * Movie Credits API Response DTOs
 */

data class CastMemberDto(
    val id: Long,
    val name: String,
    val character: String,
    val profilePath: String?
)

data class MovieCreditsResponse(
    val movieId: Long,
    val cast: List<CastMemberDto>
)
