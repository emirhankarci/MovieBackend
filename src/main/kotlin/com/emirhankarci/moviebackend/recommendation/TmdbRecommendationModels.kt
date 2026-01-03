package com.emirhankarci.moviebackend.recommendation

/**
 * TMDB recommendations endpoint response
 * GET /movie/{movie_id}/recommendations
 */
data class TmdbRecommendationsResponse(
    val page: Int,
    val results: List<TmdbRecommendedMovie>?,
    val total_pages: Int,
    val total_results: Int
)

/**
 * Individual movie from TMDB recommendations response
 */
data class TmdbRecommendedMovie(
    val id: Long,
    val title: String,
    val poster_path: String?,
    val vote_average: Double,
    val vote_count: Int,
    val release_date: String?,
    val genre_ids: List<Int>?
)
