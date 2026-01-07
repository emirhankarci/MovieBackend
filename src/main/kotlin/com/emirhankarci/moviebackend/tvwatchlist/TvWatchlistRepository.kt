package com.emirhankarci.moviebackend.tvwatchlist

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TvWatchlistRepository : JpaRepository<TvWatchlist, Long> {
    
    fun findByUserIdAndSeriesId(userId: Long, seriesId: Long): TvWatchlist?
    
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<TvWatchlist>

    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<TvWatchlist>
    
    fun existsByUserIdAndSeriesId(userId: Long, seriesId: Long): Boolean
    
    fun deleteByUserIdAndSeriesId(userId: Long, seriesId: Long): Int
}
