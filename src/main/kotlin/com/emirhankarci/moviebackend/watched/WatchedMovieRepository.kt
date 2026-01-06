package com.emirhankarci.moviebackend.watched

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WatchedMovieRepository : JpaRepository<WatchedMovie, Long> {
    fun findByUserId(userId: Long): List<WatchedMovie>
    fun findByUserIdAndMovieId(userId: Long, movieId: Long): Optional<WatchedMovie>
    fun existsByUserIdAndMovieId(userId: Long, movieId: Long): Boolean
    fun findByUserIdOrderByWatchedAtDesc(userId: Long): List<WatchedMovie>
    fun findByUserIdOrderByWatchedAtAsc(userId: Long): List<WatchedMovie>
    
    // Paginated queries
    fun findByUserIdOrderByWatchedAtDesc(userId: Long, pageable: Pageable): Page<WatchedMovie>
    fun findByUserIdOrderByWatchedAtAsc(userId: Long, pageable: Pageable): Page<WatchedMovie>
}
