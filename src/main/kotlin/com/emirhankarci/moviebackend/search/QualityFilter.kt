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
    
    fun filter(movies: List<MovieDto>): List<MovieDto> {
        val (passed, filtered) = movies.partition { isQualityMovie(it) }
        
        if (filtered.isNotEmpty()) {
            logger.debug("Filtered out {} movies that didn't meet quality criteria (rating >= {}, votes >= {})", 
                filtered.size, minRating, minVotes)
            filtered.forEach { movie ->
                logger.trace("Filtered: {} (rating={}, votes={})", 
                    movie.title, movie.rating, movie.voteCount)
            }
        }
        
        return passed
    }
    
    fun isQualityMovie(movie: MovieDto): Boolean {
        return movie.rating >= minRating && movie.voteCount >= minVotes
    }
    
    fun getMinRating(): Double = minRating
    fun getMinVotes(): Int = minVotes
}
