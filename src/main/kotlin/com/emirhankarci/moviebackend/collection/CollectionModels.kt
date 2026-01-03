package com.emirhankarci.moviebackend.collection

/**
 * TMDB movie details response with collection info
 * GET /movie/{movie_id}
 */
data class TmdbMovieWithCollection(
    val id: Long,
    val title: String,
    val release_date: String?,
    val belongs_to_collection: TmdbBelongsToCollection?
)

/**
 * Collection info from movie details response
 */
data class TmdbBelongsToCollection(
    val id: Long,
    val name: String,
    val poster_path: String?,
    val backdrop_path: String?
)

/**
 * TMDB collection details response
 * GET /collection/{collection_id}
 */
data class TmdbCollectionResponse(
    val id: Long,
    val name: String,
    val overview: String?,
    val poster_path: String?,
    val parts: List<TmdbCollectionMovie>?
)

/**
 * Individual movie from collection parts array
 */
data class TmdbCollectionMovie(
    val id: Long,
    val title: String,
    val poster_path: String?,
    val release_date: String?,
    val overview: String?
)
