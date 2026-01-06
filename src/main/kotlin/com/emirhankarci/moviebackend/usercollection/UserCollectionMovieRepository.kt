package com.emirhankarci.moviebackend.usercollection

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserCollectionMovieRepository : JpaRepository<UserCollectionMovie, Long> {
    fun findByCollectionIdOrderByAddedAtAsc(collectionId: Long): List<UserCollectionMovie>
    fun findByCollectionIdOrderByAddedAtDesc(collectionId: Long): List<UserCollectionMovie>
    fun countByCollectionId(collectionId: Long): Int
    fun findByCollectionIdAndMovieId(collectionId: Long, movieId: Long): Optional<UserCollectionMovie>
    fun existsByCollectionIdAndMovieId(collectionId: Long, movieId: Long): Boolean
    fun findFirstByCollectionIdOrderByAddedAtAsc(collectionId: Long): Optional<UserCollectionMovie>
    fun findTop4ByCollectionIdOrderByAddedAtAsc(collectionId: Long): List<UserCollectionMovie>
    
    @Query("SELECT m FROM UserCollectionMovie m WHERE m.movieId = :movieId AND m.collection.user.id = :userId")
    fun findByMovieIdAndCollectionUserId(movieId: Long, userId: Long): List<UserCollectionMovie>
    
    // Paginated queries
    fun findByCollectionIdOrderByAddedAtAsc(collectionId: Long, pageable: Pageable): Page<UserCollectionMovie>
    fun findByCollectionIdOrderByAddedAtDesc(collectionId: Long, pageable: Pageable): Page<UserCollectionMovie>
}
