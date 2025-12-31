package com.emirhankarci.moviebackend.search

import com.emirhankarci.moviebackend.search.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException

@Service
class MovieSearchService(
    private val restTemplate: RestTemplate,
    private val qualityFilter: QualityFilter
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MovieSearchService::class.java)
        private const val TMDB_API_URL = "https://api.themoviedb.org/3"
    }

    private val apiKey: String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    fun searchByName(query: String, page: Int, size: Int): SearchResult {
        return try {
            val tmdbPage = page + 1 // TMDB uses 1-based pagination
            val url = "$TMDB_API_URL/search/movie?api_key=$apiKey&query=${query.encodeUrl()}&language=tr-TR&page=$tmdbPage"
            
            logger.info("Searching TMDB for: {}", query)
            val response = restTemplate.getForObject(url, TmdbSearchResponse::class.java)
            
            val movies = response?.results?.map { it.toMovieDto() } ?: emptyList()
            val filteredMovies = qualityFilter.filter(movies)
            
            // Apply size limit
            val pagedMovies = filteredMovies.take(size)
            
            logger.info("Found {} movies, {} after quality filter, returning {}", 
                movies.size, filteredMovies.size, pagedMovies.size)
            
            SearchResult.Success(
                movies = pagedMovies,
                pagination = PaginationInfo(
                    currentPage = page,
                    totalPages = response?.total_pages ?: 0,
                    totalElements = filteredMovies.size,
                    size = size
                )
            )
        } catch (e: ResourceAccessException) {
            logger.error("TMDB API unavailable: {}", e.message)
            SearchResult.Error("EXTERNAL_SERVICE_ERROR", "Film servisi şu anda kullanılamıyor")
        } catch (e: HttpClientErrorException) {
            logger.error("TMDB API error: {}", e.message)
            SearchResult.Error("EXTERNAL_SERVICE_ERROR", "Film arama hatası")
        } catch (e: Exception) {
            logger.error("Search failed: {}", e.message)
            SearchResult.Error("SEARCH_ERROR", "Arama sırasında bir hata oluştu")
        }
    }

    fun discoverWithFilters(filters: DiscoverFilters, page: Int, size: Int): SearchResult {
        return try {
            val tmdbPage = page + 1
            val urlBuilder = StringBuilder("$TMDB_API_URL/discover/movie?api_key=$apiKey&language=tr-TR&page=$tmdbPage")
            
            // Add genre filter
            filters.genre?.let { genre ->
                TmdbGenreMapper.getGenreId(genre)?.let { genreId ->
                    urlBuilder.append("&with_genres=$genreId")
                }
            }
            
            // Add year filter
            filters.year?.let { year ->
                urlBuilder.append("&primary_release_year=$year")
            }
            
            // Sort by popularity for better results
            urlBuilder.append("&sort_by=popularity.desc")
            
            logger.info("Discovering movies with filters: {}", filters)
            val response = restTemplate.getForObject(urlBuilder.toString(), TmdbSearchResponse::class.java)
            
            var movies = response?.results?.map { it.toMovieDto() } ?: emptyList()
            
            // Apply quality filter
            movies = qualityFilter.filter(movies)
            
            // Apply rating range filters (TMDB doesn't support exact rating filters well)
            movies = applyRatingFilters(movies, filters)
            
            // Apply size limit
            val pagedMovies = movies.take(size)
            
            logger.info("Discovered {} movies after all filters, returning {}", movies.size, pagedMovies.size)
            
            SearchResult.Success(
                movies = pagedMovies,
                pagination = PaginationInfo(
                    currentPage = page,
                    totalPages = response?.total_pages ?: 0,
                    totalElements = movies.size,
                    size = size
                )
            )
        } catch (e: ResourceAccessException) {
            logger.error("TMDB API unavailable: {}", e.message)
            SearchResult.Error("EXTERNAL_SERVICE_ERROR", "Film servisi şu anda kullanılamıyor")
        } catch (e: HttpClientErrorException) {
            logger.error("TMDB API error: {}", e.message)
            SearchResult.Error("EXTERNAL_SERVICE_ERROR", "Film keşif hatası")
        } catch (e: Exception) {
            logger.error("Discover failed: {}", e.message)
            SearchResult.Error("DISCOVER_ERROR", "Keşif sırasında bir hata oluştu")
        }
    }

    private fun applyRatingFilters(movies: List<MovieDto>, filters: DiscoverFilters): List<MovieDto> {
        return movies.filter { movie ->
            val minOk = filters.minRating?.let { movie.rating >= it } ?: true
            val maxOk = filters.maxRating?.let { movie.rating <= it } ?: true
            minOk && maxOk
        }
    }

    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }

    private fun TmdbMovieResult.toMovieDto(): MovieDto {
        return MovieDto(
            id = this.id,
            title = this.title,
            posterPath = this.poster_path,
            rating = this.vote_average,
            voteCount = this.vote_count,
            releaseDate = this.release_date,
            overview = this.overview,
            genres = this.genre_ids?.map { TmdbGenreMapper.getGenreName(it) } ?: emptyList()
        )
    }
}

// TMDB Response models
data class TmdbSearchResponse(
    val results: List<TmdbMovieResult>?,
    val total_pages: Int?,
    val total_results: Int?
)

data class TmdbMovieResult(
    val id: Long,
    val title: String,
    val poster_path: String?,
    val vote_average: Double,
    val vote_count: Int,
    val release_date: String?,
    val overview: String?,
    val genre_ids: List<Int>?
)

// Search result sealed class
sealed class SearchResult {
    data class Success(
        val movies: List<MovieDto>,
        val pagination: PaginationInfo
    ) : SearchResult()
    
    data class Error(
        val code: String,
        val message: String
    ) : SearchResult()
}
