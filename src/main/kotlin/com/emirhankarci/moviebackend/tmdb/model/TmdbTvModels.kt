package com.emirhankarci.moviebackend.tmdb.model

/**
 * TMDB TV Series API Response Models
 * Bu modeller TMDB API'den gelen TV dizisi raw JSON response'ları temsil eder.
 * snake_case field isimleri Jackson tarafından otomatik map edilir.
 */

// ==================== TV Series Detail ====================

data class TmdbTvSeriesDetailResponse(
    val id: Long,
    val name: String,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val first_air_date: String?,
    val last_air_date: String?,
    val status: String?,
    val tagline: String?,
    val vote_average: Double,
    val vote_count: Int,
    val number_of_seasons: Int,
    val number_of_episodes: Int,
    val episode_run_time: List<Int>?,
    val genres: List<TmdbGenre>,
    val networks: List<TmdbNetwork>?,
    val seasons: List<TmdbSeasonSummary>?,
    val popularity: Double?
)

data class TmdbNetwork(
    val id: Int,
    val name: String,
    val logo_path: String?
)

data class TmdbSeasonSummary(
    val id: Long,
    val season_number: Int,
    val name: String?,
    val episode_count: Int,
    val air_date: String?,
    val poster_path: String?,
    val overview: String?
)

// ==================== Season Detail ====================

data class TmdbSeasonDetailResponse(
    val id: Long,
    val season_number: Int,
    val name: String?,
    val overview: String?,
    val air_date: String?,
    val poster_path: String?,
    val episodes: List<TmdbEpisodeSummary>?
)

data class TmdbEpisodeSummary(
    val id: Long,
    val episode_number: Int,
    val name: String?,
    val overview: String?,
    val air_date: String?,
    val still_path: String?,
    val vote_average: Double,
    val vote_count: Int?,
    val runtime: Int?
)


// ==================== Episode Detail ====================

data class TmdbEpisodeDetailResponse(
    val id: Long,
    val season_number: Int,
    val episode_number: Int,
    val name: String?,
    val overview: String?,
    val air_date: String?,
    val still_path: String?,
    val vote_average: Double,
    val vote_count: Int,
    val runtime: Int?,
    val guest_stars: List<TmdbGuestStar>?
)

data class TmdbGuestStar(
    val id: Long,
    val name: String,
    val character: String?,
    val profile_path: String?,
    val order: Int?
)

// ==================== TV Series Credits ====================

data class TmdbTvCreditsResponse(
    val id: Long,
    val cast: List<TmdbTvCastMember>?,
    val crew: List<TmdbTvCrewMember>?
)

data class TmdbTvCastMember(
    val id: Long,
    val name: String,
    val character: String?,
    val profile_path: String?,
    val order: Int?
)

data class TmdbTvCrewMember(
    val id: Long,
    val name: String,
    val job: String?,
    val department: String?,
    val profile_path: String?
)


// ==================== TV Series List (Popular, Top Rated, On The Air, Airing Today) ====================

data class TmdbTvListResponse(
    val page: Int,
    val results: List<TmdbTvListItem>,
    val total_pages: Int,
    val total_results: Int
)

data class TmdbTvListItem(
    val id: Long,
    val name: String,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Double,
    val vote_count: Int,
    val first_air_date: String?,
    val popularity: Double?,
    val genre_ids: List<Int>?
)
