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
class FeaturedTvSeriesService(
    private val restTemplate: RestTemplate,
    private val cacheService: CacheService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FeaturedTvSeriesService::class.java)
        private const val TMDB_API_URL = "https://api.themoviedb.org/3"
        private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/original"
        
        private const val MIN_RATING = 6.5
        private const val STRICT_VOTE_COUNT = 500
        private const val RELAXED_VOTE_COUNT = 200
        private const val MIN_TV_SERIES = 5
        private const val MIN_LIMIT = 5
        private const val MAX_LIMIT = 10
    }

    private val apiKey: String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    /**
     * Get featured TV series with Redis caching
     */
    fun getFeaturedTvSeries(timeWindow: TimeWindow, limit: Int): FeaturedTvSeriesResult {
        val effectiveLimit = clampLimit(limit)
        val cacheKey = CacheKeys.FeaturedTv.trending(timeWindow.value)
        
        // Check Redis cache first
        cacheService.get(cacheKey, FeaturedTvSeriesCacheData::class.java)?.let { cachedData ->
            logger.info("Redis cache HIT for featured TV series: {}", cacheKey)
            return FeaturedTvSeriesResult.Success(cachedData.tvSeries.take(effectiveLimit))
        }
        
        logger.info("Redis cache MISS for featured TV series: {}, fetching from TMDB", cacheKey)
        
        return try {
            val trendingTvSeries = fetchTrendingTvSeries(timeWindow)
            val filteredSeries = filterByBackdrop(trendingTvSeries)
            val qualitySeries = applyQualityFilter(filteredSeries)
            val featuredTvSeries = transformToFeaturedTvSeriesParallel(qualitySeries)
            
            // Cache the results in Redis
            val ttl = if (timeWindow == TimeWindow.DAY) CacheKeys.TTL.SHORT else CacheKeys.TTL.MEDIUM
            cacheService.set(cacheKey, FeaturedTvSeriesCacheData(featuredTvSeries), ttl)
            
            logger.info("Fetched {} featured TV series for {}", featuredTvSeries.size, timeWindow.value)
            FeaturedTvSeriesResult.Success(featuredTvSeries.take(effectiveLimit))
        } catch (e: ResourceAccessException) {
            logger.error("TMDB API unavailable: {}", e.message)
            FeaturedTvSeriesResult.Error("EXTERNAL_SERVICE_ERROR", "Dizi servisi şu anda kullanılamıyor")
        } catch (e: HttpClientErrorException) {
            logger.error("TMDB API error: {}", e.message)
            FeaturedTvSeriesResult.Error("EXTERNAL_SERVICE_ERROR", "Dizi servisi hatası")
        } catch (e: Exception) {
            logger.error("Failed to fetch featured TV series: {}", e.message)
            FeaturedTvSeriesResult.Error("FETCH_ERROR", "Öne çıkan diziler alınamadı")
        }
    }


    /**
     * Fetch trending TV series from TMDB API
     */
    private fun fetchTrendingTvSeries(timeWindow: TimeWindow): List<TmdbTrendingTvSeries> {
        val url = "$TMDB_API_URL/trending/tv/${timeWindow.value}?api_key=$apiKey&language=tr-TR"
        logger.debug("Fetching trending TV series: {}", url.replace(apiKey, "***"))
        
        val response = restTemplate.getForObject(url, TmdbTrendingTvResponse::class.java)
        return response?.results ?: emptyList()
    }

    /**
     * Filter out TV series without backdrop images
     */
    private fun filterByBackdrop(series: List<TmdbTrendingTvSeries>): List<TmdbTrendingTvSeries> {
        return series.filter { !it.backdrop_path.isNullOrBlank() }
    }

    /**
     * Apply quality filter with relaxation if needed
     */
    private fun applyQualityFilter(series: List<TmdbTrendingTvSeries>): List<TmdbTrendingTvSeries> {
        // First try strict filtering
        val strictFiltered = series.filter { 
            it.vote_average >= MIN_RATING && it.vote_count >= STRICT_VOTE_COUNT 
        }
        
        if (strictFiltered.size >= MIN_TV_SERIES) {
            logger.info("Strict quality filter: {} TV series passed", strictFiltered.size)
            return strictFiltered
        }
        
        // Relax vote count threshold
        val relaxedFiltered = series.filter { 
            it.vote_average >= MIN_RATING && it.vote_count >= RELAXED_VOTE_COUNT 
        }
        
        logger.info("Relaxed quality filter: {} TV series passed (strict had {})", 
            relaxedFiltered.size, strictFiltered.size)
        return relaxedFiltered
    }

    /**
     * Transform TMDB TV series to FeaturedTvSeries DTOs with parallel tagline fetching
     */
    private fun transformToFeaturedTvSeriesParallel(series: List<TmdbTrendingTvSeries>): List<FeaturedTvSeries> {
        val startTime = System.currentTimeMillis()
        
        val featuredTvSeries = runBlocking(Dispatchers.IO) {
            series.map { tvSeries ->
                async {
                    try {
                        val tagline = fetchTagline(tvSeries.id)
                        FeaturedTvSeries(
                            id = tvSeries.id,
                            name = tvSeries.name,
                            backdropPath = buildFullBackdropUrl(tvSeries.backdrop_path!!),
                            tagline = tagline ?: "",
                            rating = tvSeries.vote_average,
                            firstAirYear = extractYear(tvSeries.first_air_date),
                            genres = tvSeries.genre_ids?.map { TmdbGenreMapper.getGenreName(it) } ?: emptyList()
                        )
                    } catch (e: Exception) {
                        logger.warn("Failed to transform TV series {}: {}", tvSeries.id, e.message)
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
        
        val duration = System.currentTimeMillis() - startTime
        logger.info("Parallel tagline fetch for {} TV series completed in {}ms", series.size, duration)
        
        return featuredTvSeries
    }

    /**
     * Fetch tagline from TV series details endpoint
     */
    private fun fetchTagline(seriesId: Long): String? {
        return try {
            val url = "$TMDB_API_URL/tv/$seriesId?api_key=$apiKey&language=tr-TR"
            val details = restTemplate.getForObject(url, TmdbTvSeriesDetails::class.java)
            details?.tagline?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            logger.debug("Failed to fetch tagline for TV series {}: {}", seriesId, e.message)
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
     * Extract year from first air date string (YYYY-MM-DD format)
     */
    fun extractYear(firstAirDate: String?): Int {
        return firstAirDate?.take(4)?.toIntOrNull() ?: 0
    }

    /**
     * Clamp limit to valid range [5, 10]
     */
    fun clampLimit(limit: Int): Int {
        return limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
    }
}
