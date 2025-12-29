package com.emirhankarci.moviebackend.movie

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WatchlistRepository : JpaRepository<Watchlist, Long> {
    fun findByUserId(userId: Long): List<Watchlist>
    fun findByUserIdAndMovieId(userId: Long, movieId: Long): Optional<Watchlist>
    fun existsByUserIdAndMovieId(userId: Long, movieId: Long): Boolean
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Watchlist>
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Watchlist>
}
