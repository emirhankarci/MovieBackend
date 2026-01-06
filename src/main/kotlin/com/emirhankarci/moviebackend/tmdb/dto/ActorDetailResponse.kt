package com.emirhankarci.moviebackend.tmdb.dto

/**
 * Actor Detail API Response DTO
 */
data class ActorDetailResponse(
    val id: Long,
    val name: String,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    val placeOfBirth: String?,
    val profilePath: String?,
    val knownForDepartment: String?
)
