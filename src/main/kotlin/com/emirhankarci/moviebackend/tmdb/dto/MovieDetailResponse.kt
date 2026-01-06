package com.emirhankarci.moviebackend.tmdb.dto

/**
 * Movie Detail API Response DTOs
 */

data class GenreDto(
    val id: Int,
    val name: String
)

data class MovieDetailResponse(
    val id: Long,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val tagline: String?,
    val runtime: Int?,
    val releaseDate: String?,
    val voteAverage: Double,
    val originalLanguage: String,
    val genres: List<GenreDto>
)
