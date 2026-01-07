package com.emirhankarci.moviebackend.tvcollection

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TvCollectionRepository : JpaRepository<TvCollection, Long> {
    
    fun findByUserId(userId: Long): List<TvCollection>
    
    fun findByIdAndUserId(id: Long, userId: Long): TvCollection?
    
    fun existsByIdAndUserId(id: Long, userId: Long): Boolean
    
    fun deleteByIdAndUserId(id: Long, userId: Long): Int
}
