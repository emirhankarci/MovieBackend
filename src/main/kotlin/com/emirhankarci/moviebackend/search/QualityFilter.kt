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
     */
    fun filter(movies: List<MovieDto>, sortType: SortType? = null): List<MovieDto> {
        val effectiveMinVotes = getMinVotesForSort(sortType)
        val (passed, filtered) = movies.partition { isQualityMovie(it, effectiveMinVotes) }
        
        if (filtered.isNotEmpty()) {
            logger.debug("Filtered out {} movies that didn't meet quality criteria (rating >= {}, votes >= {}, sortType={})", 
                filtered.size, minRating, effectiveMinVotes, sortType)
            filtered.forEach { movie ->
                logger.trace("Filtered: {} (rating={}, votes={})", 
                    movie.title, movie.rating, movie.voteCount)
            }
        }
        
        return passed
    }
    
    /**
     * Sort tipine göre minimum oy sayısını döndürür.
     * - RELEASE_DATE: 100 (yeni filmler için gevşek)
     * - VOTE_AVERAGE: 500 (orta seviye)
     * - POPULARITY, VOTE_COUNT: 1000 (standart)
     */
    fun getMinVotesForSort(sortType: SortType?): Int {
        return when (sortType) {
            SortType.RELEASE_DATE -> 100
            SortType.VOTE_AVERAGE -> 500
            SortType.POPULARITY, SortType.VOTE_COUNT -> minVotes
            null -> minVotes
        }
    }
    
    fun isQualityMovie(movie: MovieDto): Boolean {
        return movie.rating >= minRating && movie.voteCount >= minVotes
    }
    
    private fun isQualityMovie(movie: MovieDto, effectiveMinVotes: Int): Boolean {
        return movie.rating >= minRating && movie.voteCount >= effectiveMinVotes
    }
    
    fun getMinRating(): Double = minRating
    fun getMinVotes(): Int = minVotes
}
