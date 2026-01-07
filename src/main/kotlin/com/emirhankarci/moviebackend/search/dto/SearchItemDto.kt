package com.emirhankarci.moviebackend.search.dto

data class SearchItemDto(
    val id: Long,
    val title: String?, // title for movie, name for tv/person
    val posterPath: String?, // poster_path for movie/tv, profile_path for person
    val mediaType: String, // "movie", "tv", "person"
    val releaseDate: String?, // release_date for movie, first_air_date for tv
    val rating: Double?,
    val overview: String?
)
