package com.emirhankarci.moviebackend.search.dto

data class MovieDto(
    val id: Long,
    val title: String,
    val posterPath: String?,
    val rating: Double,
    val voteCount: Int,
    val releaseDate: String?,
    val overview: String?,
    val genres: List<String>
)

data class SearchResponse(
    val movies: List<MovieDto>,
    val pagination: PaginationInfo
)

data class PaginationInfo(
    val currentPage: Int,
    val totalPages: Int,
    val totalElements: Int,
    val size: Int
)
