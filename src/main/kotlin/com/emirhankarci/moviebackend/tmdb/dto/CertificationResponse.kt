package com.emirhankarci.moviebackend.tmdb.dto

data class CertificationResponse(
    val movieId: Long,
    val certification: String,
    val region: String
)
