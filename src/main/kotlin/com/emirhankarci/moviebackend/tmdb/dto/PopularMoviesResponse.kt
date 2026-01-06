package com.emirhankarci.moviebackend.tmdb.dto

/**
 * Popular Movies API Response DTOs
 * Mobil uygulamaya döndürülen response modelleri.
 */

data class PopularMovieDto(
    val id: Long,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val rating: Double,
    val releaseDate: String?
)

data class PopularMoviesResponse(
    val movies: List<PopularMovieDto>,
    val page: Int,
    val totalPages: Int,
    val totalResults: Int
)
