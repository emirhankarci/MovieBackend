package com.emirhankarci.moviebackend.search

import com.emirhankarci.moviebackend.search.dto.MovieDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class QualityFilter(
    @Value("\${movie.quality.min-rating:6.5}") 
    private val minRating: Double,
    
    @Value("\${movie.quality.min-votes:1000}") 
    private val minVotes: Int
) {
    companion object {
        private val logger = LoggerFactory.getLogger(QualityFilter::class.java)
    }
    
    /**
     * Filmleri kalite kriterlerine göre filtreler.
     * Sort tipi belirtilmezse standart kriterler uygulanır.
     * sortBy parametresi asc/desc bilgisini içerir.
     */
    fun filter(movies: List<MovieDto>, sortType: SortType? = null, sortBy: String? = null): List<MovieDto> {
        val effectiveMinVotes = getMinVotesForSort(sortType, sortBy)
        val effectiveMinRating = getMinRatingForSort(sortType, sortBy)
        
        val (passed, filtered) = movies.partition { isQualityMovie(it, effectiveMinVotes, effectiveMinRating) }
        
        if (filtered.isNotEmpty()) {
            logger.debug("Filtered out {} movies (rating >= {}, votes >= {}, sortType={}, sortBy={})", 
                filtered.size, effectiveMinRating, effectiveMinVotes, sortType, sortBy)
            filtered.forEach { movie ->
                logger.trace("Filtered: {} (rating={}, votes={})", 
                    movie.title, movie.rating, movie.voteCount)
            }
        }
        
        return passed
    }
    
    /**
     * Sort tipine göre minimum oy sayısını döndürür.
     * - RELEASE_DATE desc: 50 (en yeni filmler için çok gevşek)
     * - RELEASE_DATE asc: 100
     * - VOTE_AVERAGE: 100 (düşük puanlı filmler için)
     * - POPULARITY, VOTE_COUNT: 1000 (standart)
     */
    fun getMinVotesForSort(sortType: SortType?, sortBy: String? = null): Int {
        return when (sortType) {
            SortType.RELEASE_DATE -> if (sortBy?.endsWith(".desc") == true) 50 else 100
            SortType.VOTE_AVERAGE -> 100
            SortType.POPULARITY, SortType.VOTE_COUNT -> minVotes
            null -> minVotes
        }
    }
    
    /**
     * Sort tipine göre minimum rating'i döndürür.
     * - VOTE_AVERAGE asc: 0.0 (düşük puanlı filmler için rating filtresi yok)
     * - Diğerleri: 6.5 (standart)
     */
    fun getMinRatingForSort(sortType: SortType?, sortBy: String? = null): Double {
        // Lowest Rated (vote_average.asc) için rating threshold'u kaldır
        if (sortType == SortType.VOTE_AVERAGE && sortBy?.endsWith(".asc") == true) {
            return 0.0
        }
        return minRating
    }
    
    fun isQualityMovie(movie: MovieDto): Boolean {
        return movie.rating >= minRating && movie.voteCount >= minVotes
    }
    
    private fun isQualityMovie(movie: MovieDto, effectiveMinVotes: Int, effectiveMinRating: Double): Boolean {
        return movie.rating >= effectiveMinRating && movie.voteCount >= effectiveMinVotes
    }
    
    fun getMinRating(): Double = minRating
    fun getMinVotes(): Int = minVotes
}
