package com.emirhankarci.moviebackend.featured

/**
 * Time window for trending movies
 */
enum class TimeWindow(val value: String, val cacheTtlHours: Long) {
    DAY("day", 1),
    WEEK("week", 6);

    companion object {
        fun fromString(value: String): TimeWindow =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: DAY
        
        fun isValid(value: String): Boolean =
            entries.any { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * TMDB Trending API response
 */
data class TmdbTrendingResponse(
    val results: List<TmdbTrendingMovie>?,
    val page: Int?,
    val total_pages: Int?
)

/**
 * TMDB Trending movie item
 */
data class TmdbTrendingMovie(
    val id: Long,
    val title: String,
    val backdrop_path: String?,
    val vote_average: Double,
    val vote_count: Int,
    val release_date: String?,
    val genre_ids: List<Int>?,
    val overview: String?
)

/**
 * TMDB Movie details response (for tagline)
 */
data class TmdbMovieDetails(
    val id: Long,
    val tagline: String?
)
