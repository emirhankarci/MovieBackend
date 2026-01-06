package com.emirhankarci.moviebackend.featured

import com.emirhankarci.moviebackend.cache.CacheKeys
import com.emirhankarci.moviebackend.cache.CacheService
import com.emirhankarci.moviebackend.search.TmdbGenreMapper
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate

@Service
class FeaturedMoviesService(
    private val restTemplate: RestTemplate,
    private val cacheService: CacheService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FeaturedMoviesService::class.java)
        private const val TMDB_API_URL = "https://api.themoviedb.org/3"
        private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/original"
        
        private const val MIN_RATING = 6.5
        private const val STRICT_VOTE_COUNT = 500
        private const val RELAXED_VOTE_COUNT = 200
        private const val MIN_MOVIES = 5
        private const val MIN_LIMIT = 5
        private const val MAX_LIMIT = 10
    }

    private val apiKey: String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    /**
     * Get featured movies with Redis caching
     */
    fun getFeaturedMovies(timeWindow: TimeWindow, limit: Int): FeaturedMoviesResult {
        val effectiveLimit = clampLimit(limit)
        val cacheKey = CacheKeys.Featured.trending(timeWindow.value)
        
        // Check Redis cache first
        cacheService.get(cacheKey, FeaturedMoviesCacheData::class.java)?.let { cachedData ->
            logger.info("Redis cache HIT for featured movies: {}", cacheKey)
            return FeaturedMoviesResult.Success(cachedData.movies.take(effectiveLimit))
        }
        
        logger.info("Redis cache MISS for featured movies: {}, fetching from TMDB", cacheKey)
        
        return try {
            val trendingMovies = fetchTrendingMovies(timeWindow)
            val filteredMovies = filterByBackdrop(trendingMovies)
            val qualityMovies = applyQualityFilter(filteredMovies)
            val featuredMovies = transformToFeaturedMoviesParallel(qualityMovies)
            
            // Cache the results in Redis
            val ttl = if (timeWindow == TimeWindow.DAY) CacheKeys.TTL.SHORT else CacheKeys.TTL.MEDIUM
            cacheService.set(cacheKey, FeaturedMoviesCacheData(featuredMovies), ttl)
            
            logger.info("Fetched {} featured movies for {}", featuredMovies.size, timeWindow.value)
            FeaturedMoviesResult.Success(featuredMovies.take(effectiveLimit))
        } catch (e: ResourceAccessException) {
            logger.error("TMDB API unavailable: {}", e.message)
            FeaturedMoviesResult.Error("EXTERNAL_SERVICE_ERROR", "Film servisi şu anda kullanılamıyor")
        } catch (e: HttpClientErrorException) {
            logger.error("TMDB API error: {}", e.message)
            FeaturedMoviesResult.Error("EXTERNAL_SERVICE_ERROR", "Film servisi hatası")
        } catch (e: Exception) {
            logger.error("Failed to fetch featured movies: {}", e.message)
            FeaturedMoviesResult.Error("FETCH_ERROR", "Öne çıkan filmler alınamadı")
        }
    }

    /**
     * Fetch trending movies from TMDB API
     */
    private fun fetchTrendingMovies(timeWindow: TimeWindow): List<TmdbTrendingMovie> {
        val url = "$TMDB_API_URL/trending/movie/${timeWindow.value}?api_key=$apiKey&language=tr-TR"
        logger.debug("Fetching trending movies: {}", url.replace(apiKey, "***"))
        
        val response = restTemplate.getForObject(url, TmdbTrendingResponse::class.java)
        return response?.results ?: emptyList()
    }

    /**
     * Filter out movies without backdrop images
     */
    private fun filterByBackdrop(movies: List<TmdbTrendingMovie>): List<TmdbTrendingMovie> {
        return movies.filter { !it.backdrop_path.isNullOrBlank() }
    }

    /**
     * Apply quality filter with relaxation if needed
     */
    private fun applyQualityFilter(movies: List<TmdbTrendingMovie>): List<TmdbTrendingMovie> {
        // First try strict filtering
        val strictFiltered = movies.filter { 
            it.vote_average >= MIN_RATING && it.vote_count >= STRICT_VOTE_COUNT 
        }
        
        if (strictFiltered.size >= MIN_MOVIES) {
            logger.info("Strict quality filter: {} movies passed", strictFiltered.size)
            return strictFiltered
        }
        
        // Relax vote count threshold
        val relaxedFiltered = movies.filter { 
            it.vote_average >= MIN_RATING && it.vote_count >= RELAXED_VOTE_COUNT 
        }
        
        logger.info("Relaxed quality filter: {} movies passed (strict had {})", 
            relaxedFiltered.size, strictFiltered.size)
        return relaxedFiltered
    }

    /**
     * Transform TMDB movies to FeaturedMovie DTOs with parallel tagline fetching
     */
    private fun transformToFeaturedMoviesParallel(movies: List<TmdbTrendingMovie>): List<FeaturedMovie> {
        val startTime = System.currentTimeMillis()
        
        val featuredMovies = runBlocking(Dispatchers.IO) {
            movies.map { movie ->
                async {
                    try {
                        val tagline = fetchTagline(movie.id)
                        FeaturedMovie(
                            id = movie.id,
                            title = movie.title,
                            backdropPath = buildFullBackdropUrl(movie.backdrop_path!!),
                            tagline = tagline ?: "",
                            rating = movie.vote_average,
                            releaseYear = extractYear(movie.release_date),
                            genres = movie.genre_ids?.map { TmdbGenreMapper.getGenreName(it) } ?: emptyList()
                        )
                    } catch (e: Exception) {
                        logger.warn("Failed to transform movie {}: {}", movie.id, e.message)
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
        
        val duration = System.currentTimeMillis() - startTime
        logger.info("Parallel tagline fetch for {} movies completed in {}ms", movies.size, duration)
        
        return featuredMovies
    }

    /**
     * Fetch tagline from movie details endpoint
     */
    private fun fetchTagline(movieId: Long): String? {
        return try {
            val url = "$TMDB_API_URL/movie/$movieId?api_key=$apiKey&language=tr-TR"
            val details = restTemplate.getForObject(url, TmdbMovieDetails::class.java)
            details?.tagline?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            logger.debug("Failed to fetch tagline for movie {}: {}", movieId, e.message)
            null
        }
    }

    /**
     * Build full TMDB image URL from backdrop path
     */
    fun buildFullBackdropUrl(backdropPath: String): String {
        return "$TMDB_IMAGE_BASE_URL$backdropPath"
    }

    /**
     * Extract year from release date string (YYYY-MM-DD format)
     */
    fun extractYear(releaseDate: String?): Int {
        return releaseDate?.take(4)?.toIntOrNull() ?: 0
    }

    /**
     * Clamp limit to valid range [5, 10]
     */
    fun clampLimit(limit: Int): Int {
        return limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
    }
}
