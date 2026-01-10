package com.emirhankarci.moviebackend.tmdb.model

/**
 * TMDB API Response Models
 * Bu modeller TMDB API'den gelen raw JSON response'ları temsil eder.
 * snake_case field isimleri Jackson tarafından otomatik map edilir.
 */

// ==================== Popular Movies ====================

data class TmdbPopularResponse(
    val page: Int,
    val results: List<TmdbMovieResult>,
    val total_pages: Int,
    val total_results: Int
)

data class TmdbMovieResult(
    val id: Long,
    val title: String,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Double,
    val vote_count: Int,
    val release_date: String?,
    val popularity: Double?
)

// ==================== Movie Detail ====================

data class TmdbMovieDetailResponse(
    val id: Long,
    val title: String,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val tagline: String?,
    val runtime: Int?,
    val release_date: String?,
    val vote_average: Double,
    val vote_count: Int,
    val original_language: String,
    val genres: List<TmdbGenre>,
    val status: String?,
    val budget: Long?,
    val revenue: Long?
)

data class TmdbGenre(
    val id: Int,
    val name: String
)

// ==================== Movie Credits ====================

data class TmdbCreditsResponse(
    val id: Long,
    val cast: List<TmdbCastMember>,
    val crew: List<TmdbCrewMember>?
)

data class TmdbCastMember(
    val id: Long,
    val name: String,
    val character: String?,
    val profile_path: String?,
    val order: Int?
)

data class TmdbCrewMember(
    val id: Long,
    val name: String,
    val job: String?,
    val department: String?,
    val profile_path: String?
)

// ==================== Movie Recommendations ====================

data class TmdbRecommendationsResponse(
    val page: Int,
    val results: List<TmdbMovieResult>,
    val total_pages: Int,
    val total_results: Int
)


// ==================== Actor Detail ====================

data class TmdbActorDetailResponse(
    val id: Long,
    val name: String,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    val place_of_birth: String?,
    val profile_path: String?,
    val known_for_department: String?,
    val popularity: Double?
)

// ==================== Actor Movie Credits ====================

data class TmdbActorMovieCreditsResponse(
    val id: Long,
    val cast: List<TmdbActorMovieCredit>
)

data class TmdbActorMovieCredit(
    val id: Long,
    val title: String,
    val character: String?,
    val poster_path: String?,
    val release_date: String?,
    val vote_average: Double,
    val vote_count: Int?
)

// ==================== Release Dates ====================

data class TmdbReleaseDatesResponse(
    val id: Long,
    val results: List<TmdbReleaseDateResult>
)

data class TmdbReleaseDateResult(
    val iso_3166_1: String,
    val release_dates: List<TmdbReleaseDate>
)

data class TmdbReleaseDate(
    val certification: String?,
    val iso_639_1: String?,
    val note: String?,
    val release_date: String?,
    val type: Int?
)


// ==================== Actor TV Credits ====================

data class TmdbActorTvCreditsResponse(
    val id: Long,
    val cast: List<TmdbActorTvCredit>
)

data class TmdbActorTvCredit(
    val id: Long,
    val name: String,
    val character: String?,
    val poster_path: String?,
    val first_air_date: String?,
    val vote_average: Double,
    val vote_count: Int?,
    val episode_count: Int?
)

// ==================== Actor External IDs ====================

data class TmdbActorExternalIdsResponse(
    val id: Long,
    val instagram_id: String?,
    val twitter_id: String?
)
