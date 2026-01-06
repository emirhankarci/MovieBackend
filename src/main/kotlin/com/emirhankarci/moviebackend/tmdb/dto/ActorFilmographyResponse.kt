package com.emirhankarci.moviebackend.tmdb.dto

/**
 * Actor Filmography API Response DTOs
 */
data class ActorMovieCreditDto(
    val id: Long,
    val title: String,
    val character: String?,
    val posterPath: String?,
    val releaseDate: String?,
    val voteAverage: Double
)

data class ActorFilmographyResponse(
    val actorId: Long,
    val actorName: String,
    val movies: List<ActorMovieCreditDto>
)
