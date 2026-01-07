package com.emirhankarci.moviebackend.search

import com.emirhankarci.moviebackend.cache.CacheKeys
import com.emirhankarci.moviebackend.cache.CacheService
import com.emirhankarci.moviebackend.common.ImageUrlBuilder
import com.emirhankarci.moviebackend.search.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import java.security.MessageDigest

@Service
class MovieSearchService(
    private val restTemplate: RestTemplate,
    private val qualityFilter: QualityFilter,
    private val cacheService: CacheService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MovieSearchService::class.java)
        private const val TMDB_API_URL = "https://api.themoviedb.org/3"
    }

    private val apiKey: String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    fun searchByName(query: String, page: Int, size: Int): SearchResult {
        val cacheKey = CacheKeys.Search.results(query, page)
        
        // Check Redis cache first
        cacheService.get(cacheKey, SearchCacheData::class.java)?.let { cachedData ->
            logger.info("Redis cache HIT for search: {}", cacheKey)
            val pagedResults = cachedData.results.take(size)
            return SearchResult.Success(
                results = pagedResults,
                pagination = cachedData.pagination.copy(size = size)
            )
        }
        
        logger.info("Redis cache MISS for search: {}, fetching from TMDB", cacheKey)
        
        return try {
            val tmdbPage = page + 1 // TMDB uses 1-based pagination
            val url = "$TMDB_API_URL/search/multi?api_key=$apiKey&query=${query.encodeUrl()}&language=tr-TR&page=$tmdbPage"
            
            logger.info("Searching TMDB (multi) for: {}", query)
            val response = restTemplate.getForObject(url, TmdbSearchResponse::class.java)
            
            val results = response?.results?.mapNotNull { it.toSearchItemDto() } ?: emptyList()
            // Note: Quality filter might need adjustment for mixed types, currently skipping for non-movies or applying selective filtering
            // For now, we return everything from multi-search as it's more relevant
            
            // Cache all results
            val pagination = PaginationInfo(
                currentPage = page,
                totalPages = response?.total_pages ?: 0,
                totalElements = results.size,
                size = size
            )
            cacheService.set(cacheKey, SearchCacheData(results, pagination), CacheKeys.TTL.VERY_SHORT)
            
            // Apply size limit
            val pagedResults = results.take(size)
            
            logger.info("Found {} items, returning {}", results.size, pagedResults.size)
            
            SearchResult.Success(
                results = pagedResults,
                pagination = pagination
            )
        } catch (e: ResourceAccessException) {
            logger.error("TMDB API unavailable: {}", e.message)
            SearchResult.Error("EXTERNAL_SERVICE_ERROR", "Arama servisi şu anda kullanılamıyor")
        } catch (e: HttpClientErrorException) {
            logger.error("TMDB API error: {}", e.message)
            SearchResult.Error("EXTERNAL_SERVICE_ERROR", "Arama hatası")
        } catch (e: Exception) {
            logger.error("Search failed: {}", e.message)
            SearchResult.Error("SEARCH_ERROR", "Arama sırasında bir hata oluştu")
        }
    }

    fun discoverWithFilters(filters: DiscoverFilters, page: Int, size: Int): SearchResult {
        val filtersHash = generateFiltersHash(filters)
        val cacheKey = CacheKeys.Search.discover(filtersHash, page)
        
        // Check Redis cache first
        cacheService.get(cacheKey, SearchCacheData::class.java)?.let { cachedData ->
            logger.info("Redis cache HIT for discover: {}", cacheKey)
            val pagedResults = cachedData.results.take(size)
            return SearchResult.Success(
                results = pagedResults,
                pagination = cachedData.pagination.copy(size = size)
            )
        }
        
        logger.info("Redis cache MISS for discover: {}, fetching from TMDB", cacheKey)
        
        return try {
            val tmdbPage = page + 1
            val sortType = SortType.fromSortBy(filters.sortBy)
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
            
            // Sort by the specified sort option
            urlBuilder.append("&sort_by=${filters.sortBy}")
            
            // Add sort-specific parameters (release_date.lte, vote_count.gte, etc.)
            val sortParams = SortParameterBuilder.buildParameters(filters.sortBy)
            sortParams.forEach { (key, value) ->
                urlBuilder.append("&$key=$value")
            }
            
            logger.info("Discovering movies with filters: {}, sortType: {}", filters, sortType)
            logger.debug("TMDB URL: {}", urlBuilder.toString().replace(apiKey, "***"))
            
            val response = restTemplate.getForObject(urlBuilder.toString(), TmdbSearchResponse::class.java)
            
            var movies = response?.results?.map { it.toMovieDto() } ?: emptyList()
            
            // Apply quality filter with sort-specific thresholds
            movies = qualityFilter.filter(movies, sortType, filters.sortBy)
            
            // Apply rating range filters (TMDB doesn't support exact rating filters well)
            movies = applyRatingFilters(movies, filters)
            
            // Convert to SearchItemDto
            val results = movies.map { movie ->
                SearchItemDto(
                    id = movie.id,
                    title = movie.title,
                    posterPath = movie.posterPath,
                    mediaType = "movie",
                    releaseDate = movie.releaseDate,
                    rating = movie.rating,
                    overview = movie.overview
                )
            }
            
            // Cache all filtered results
            val pagination = PaginationInfo(
                currentPage = page,
                totalPages = response?.total_pages ?: 0,
                totalElements = results.size,
                size = size
            )
            cacheService.set(cacheKey, SearchCacheData(results, pagination), CacheKeys.TTL.VERY_SHORT)
            
            // Apply size limit
            val pagedResults = results.take(size)
            
            logger.info("Discovered {} movies after all filters, returning {}", movies.size, pagedResults.size)
            
            SearchResult.Success(
                results = pagedResults,
                pagination = pagination
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

    private fun generateFiltersHash(filters: DiscoverFilters): String {
        val filterString = "${filters.genre}_${filters.year}_${filters.minRating}_${filters.maxRating}_${filters.sortBy}"
        val bytes = MessageDigest.getInstance("MD5").digest(filterString.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(12)
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

    private fun TmdbMovieResult.toSearchItemDto(): SearchItemDto? {
        val mediaType = this.media_type ?: "movie" // Default to movie if missing
        
        // Determine title/name
        val title = this.title ?: this.name
        if (title.isNullOrBlank()) return null // Skip items without title/name

        // Determine date
        val date = this.release_date ?: this.first_air_date
        
        // Determine image
        val imagePath = this.poster_path ?: this.profile_path
        val fullImagePath = if (imagePath != null) ImageUrlBuilder.buildPosterUrl(imagePath) else null

        return SearchItemDto(
            id = this.id,
            title = title,
            posterPath = fullImagePath,
            mediaType = mediaType,
            releaseDate = date,
            rating = this.vote_average,
            overview = this.overview
        )
    }

    private fun TmdbMovieResult.toMovieDto(): MovieDto {
        return MovieDto(
            id = this.id,
            title = this.title ?: "",
            posterPath = ImageUrlBuilder.buildPosterUrl(this.poster_path),
            rating = this.vote_average ?: 0.0,
            voteCount = this.vote_count ?: 0,
            releaseDate = this.release_date,
            overview = this.overview,
            genres = this.genre_ids?.map { TmdbGenreMapper.getGenreName(it) } ?: emptyList()
        )
    }
}

// Cache data wrapper
data class SearchCacheData(
    val results: List<SearchItemDto>,
    val pagination: PaginationInfo
)

// TMDB Response models
data class TmdbSearchResponse(
    val results: List<TmdbMovieResult>?,
    val total_pages: Int?,
    val total_results: Int?
)

data class TmdbMovieResult(
    val id: Long,
    val title: String?, // movie
    val name: String?, // tv/person
    val poster_path: String?, // movie/tv
    val profile_path: String?, // person
    val media_type: String?, // movie/tv/person
    val vote_average: Double?,
    val vote_count: Int?,
    val release_date: String?, // movie
    val first_air_date: String?, // tv
    val overview: String?,
    val genre_ids: List<Int>?
)

// Search result sealed class
sealed class SearchResult {
    data class Success(
        val results: List<SearchItemDto>, // changed from movies: List<MovieDto>
        val pagination: PaginationInfo
    ) : SearchResult()
    
    data class Error(
        val code: String,
        val message: String
    ) : SearchResult()
}
