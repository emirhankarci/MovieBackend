package com.emirhankarci.moviebackend.tvsuggestion

import com.emirhankarci.moviebackend.cache.CacheKeys
import com.emirhankarci.moviebackend.cache.CacheService
import com.emirhankarci.moviebackend.common.ImageUrlBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class TmdbTvService(
    private val cacheService: CacheService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TmdbTvService::class.java)
        private const val TMDB_API_URL = "https://api.themoviedb.org/3"
        private const val MIN_RATING = 6.5
        private const val MIN_VOTE_COUNT = 200
    }

    private val restTemplate = RestTemplate()
    private val apiKey: String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    fun searchTvSeries(seriesTitle: String): TvSeriesData? {
        val cacheKey = "tv:search:${seriesTitle.lowercase().replace(" ", "_")}"

        // Check Redis cache first
        cacheService.get(cacheKey, TvSeriesData::class.java)?.let { cached ->
            logger.info("Redis cache HIT for TV search: {}", seriesTitle)
            return cached
        }

        logger.info("Redis cache MISS for TV search: {}, fetching from TMDB", seriesTitle)

        return try {
            // First try with Turkish language
            var series = searchTvSeriesWithLanguage(seriesTitle, "tr-TR")

            // If not found, try with English
            if (series == null) {
                logger.info("TV series not found in Turkish, trying English for: {}", seriesTitle)
                series = searchTvSeriesWithLanguage(seriesTitle, "en-US")
            }

            // If still not found, try without year/extra info
            if (series == null && seriesTitle.contains("(")) {
                val cleanTitle = seriesTitle.substringBefore("(").trim()
                logger.info("Trying with cleaned title: {}", cleanTitle)
                series = searchTvSeriesWithLanguage(cleanTitle, "en-US")
            }

            // Cache the result if found
            series?.let {
                cacheService.set(cacheKey, it, CacheKeys.TTL.SHORT)
            }

            series
        } catch (e: Exception) {
            logger.error("TMDB TV search failed for '{}': {}", seriesTitle, e.message)
            null
        }
    }

    private fun searchTvSeriesWithLanguage(seriesTitle: String, language: String): TvSeriesData? {
        return try {
            val url = "$TMDB_API_URL/search/tv?api_key=$apiKey&query=${seriesTitle.encodeUrl()}&language=$language"
            logger.debug("TMDB TV search URL: {}", url.replace(apiKey, "***"))

            val response = restTemplate.getForObject(url, TmdbTvSearchResponse::class.java)
            logger.debug("TMDB TV response: {} results", response?.results?.size ?: 0)

            val series = response?.results?.firstOrNull()
            if (series != null) {
                logger.info("Found TV series: {} (ID: {}) with language {}", series.name, series.id, language)
                TvSeriesData(
                    id = series.id,
                    name = series.name,
                    posterPath = ImageUrlBuilder.buildPosterUrl(series.poster_path),
                    rating = series.vote_average,
                    voteCount = series.vote_count
                )
            } else {
                logger.debug("TV series not found with language {}: {}", language, seriesTitle)
                null
            }
        } catch (e: Exception) {
            logger.error("TMDB TV search failed for '{}' ({}): {}", seriesTitle, language, e.message)
            null
        }
    }

    fun getTvSeriesById(seriesId: Long): TvSeriesData? {
        val cacheKey = "tv:detail:$seriesId"

        // Check Redis cache first
        cacheService.get(cacheKey, TvSeriesData::class.java)?.let { cached ->
            logger.info("Redis cache HIT for TV detail: {}", seriesId)
            return cached
        }

        return try {
            val url = "$TMDB_API_URL/tv/$seriesId?api_key=$apiKey&language=tr-TR"
            val detail = restTemplate.getForObject(url, TmdbTvDetailResponse::class.java)

            if (detail != null) {
                logger.info("Fetched TV series by ID: {} ({})", detail.name, seriesId)
                val seriesData = TvSeriesData(
                    id = detail.id,
                    name = detail.name,
                    posterPath = ImageUrlBuilder.buildPosterUrl(detail.poster_path),
                    rating = detail.vote_average,
                    voteCount = detail.vote_count
                )
                cacheService.set(cacheKey, seriesData, CacheKeys.TTL.MEDIUM)
                seriesData
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("TMDB TV fetch by ID failed for '{}': {}", seriesId, e.message)
            null
        }
    }

    fun validateTvSeriesQuality(series: TvSeriesData): Boolean {
        val isValid = series.rating >= MIN_RATING && series.voteCount >= MIN_VOTE_COUNT
        if (!isValid) {
            logger.info(
                "TV series {} failed quality check: rating={}, votes={} (need >={} rating, >={} votes)",
                series.name, series.rating, series.voteCount, MIN_RATING, MIN_VOTE_COUNT
            )
        } else {
            logger.info(
                "TV series {} passed quality check: rating={}, votes={}",
                series.name, series.rating, series.voteCount
            )
        }
        return isValid
    }

    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }
}

// TMDB TV API Response models
data class TmdbTvSearchResponse(
    val results: List<TmdbTvSeries>?
)

data class TmdbTvSeries(
    val id: Long,
    val name: String,
    val poster_path: String?,
    val vote_average: Double,
    val vote_count: Int
)

data class TmdbTvDetailResponse(
    val id: Long,
    val name: String,
    val poster_path: String?,
    val vote_average: Double,
    val vote_count: Int
)

// TV Series Data model
data class TvSeriesData(
    val id: Long,
    val name: String,
    val posterPath: String?,
    val rating: Double,
    val voteCount: Int
)
