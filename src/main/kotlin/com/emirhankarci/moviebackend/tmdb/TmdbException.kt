package com.emirhankarci.moviebackend.tmdb

/**
 * TMDB API Exception
 * TMDB API hatalarını temsil eder.
 */
class TmdbApiException(
    override val message: String,
    val statusCode: Int
) : RuntimeException(message)
