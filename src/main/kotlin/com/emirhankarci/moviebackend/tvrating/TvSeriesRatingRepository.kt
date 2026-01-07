package com.emirhankarci.moviebackend.tvrating

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TvSeriesRatingRepository : JpaRepository<TvSeriesRating, Long> {
    
    fun findByUserIdAndSeriesId(userId: Long, seriesId: Long): TvSeriesRating?
    
    fun existsByUserIdAndSeriesId(userId: Long, seriesId: Long): Boolean
    
    fun findByUserIdOrderByRatedAtDesc(userId: Long, pageable: Pageable): Page<TvSeriesRating>
    
    fun deleteByUserIdAndSeriesId(userId: Long, seriesId: Long): Int
}
