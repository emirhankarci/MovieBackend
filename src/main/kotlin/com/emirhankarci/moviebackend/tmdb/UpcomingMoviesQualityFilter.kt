package com.emirhankarci.moviebackend.tmdb

import com.emirhankarci.moviebackend.tmdb.model.TmdbMovieResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Upcoming movies için kalite filtresi.
 * API seviyesinde ve uygulama seviyesinde filtreleme sağlar.
 */
@Component
class UpcomingMoviesQualityFilter(
    @Value("\${upcoming.quality.min-vote-count:10}")
    private val minVoteCount: Int,
    
    @Value("\${upcoming.quality.min-popularity:5.0}")
    private val minPopularity: Double,
    
    @Value("\${upcoming.quality.min-runtime:60}")
    private val minRuntime: Int,
    
    @Value("\${upcoming.quality.excluded-genres:99}")
    private val excludedGenresConfig: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UpcomingMoviesQualityFilter::class.java)
    }
    
    /**
     * Hariç tutulacak tür ID'lerini parse eder.
     * Config'den virgülle ayrılmış string olarak gelir.
     */
    fun getExcludedGenreIds(): List<Int> {
        return excludedGenresConfig
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }
    
    /**
     * API seviyesinde uygulanacak filtre parametrelerini döndürür.
     * Bu parametreler TMDB Discover API'ye gönderilir.
     */
    fun getApiFilterParams(): UpcomingFilterParams {
        return UpcomingFilterParams(
            minVoteCount = minVoteCount,
            minRuntime = minRuntime,
            excludedGenreIds = getExcludedGenreIds()
        )
    }
    
    /**
     * Film listesini kalite kriterlerine göre filtreler.
     * API seviyesinde filtrelenemeyen kriterleri uygular:
     * - Poster zorunluluğu
     * - Minimum popülerlik
     */
    fun filter(movies: List<TmdbMovieResult>): List<TmdbMovieResult> {
        val (passed, filtered) = movies.partition { isQualityMovie(it) }
        
        if (filtered.isNotEmpty()) {
            logger.debug(
                "Filtered out {} upcoming movies (minPopularity={}, posterRequired=true)",
                filtered.size, minPopularity
            )
            filtered.forEach { movie ->
                logger.trace(
                    "Filtered: {} (popularity={}, hasPoster={})",
                    movie.title, movie.popularity, movie.poster_path != null
                )
            }
        }
        
        return passed
    }
    
    /**
     * Tek bir filmin kalite kriterlerini karşılayıp karşılamadığını kontrol eder.
     * API seviyesinde filtrelenemeyen kriterleri kontrol eder.
     */
    fun isQualityMovie(movie: TmdbMovieResult): Boolean {
        // Poster zorunlu
        if (movie.poster_path.isNullOrBlank()) {
            return false
        }
        
        // Minimum popülerlik kontrolü
        val popularity = movie.popularity ?: 0.0
        if (popularity < minPopularity) {
            return false
        }
        
        return true
    }
    
    // Getter methods for testing
    fun getMinVoteCount(): Int = minVoteCount
    fun getMinPopularity(): Double = minPopularity
    fun getMinRuntime(): Int = minRuntime
}
