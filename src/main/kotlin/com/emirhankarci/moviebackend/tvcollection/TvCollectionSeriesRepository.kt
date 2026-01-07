package com.emirhankarci.moviebackend.tvcollection

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TvCollectionSeriesRepository : JpaRepository<TvCollectionSeries, Long> {
    
    fun findByCollectionId(collectionId: Long, pageable: Pageable): Page<TvCollectionSeries>
    
    fun findByCollectionIdAndSeriesId(collectionId: Long, seriesId: Long): TvCollectionSeries?
    
    fun existsByCollectionIdAndSeriesId(collectionId: Long, seriesId: Long): Boolean
    
    fun deleteByCollectionIdAndSeriesId(collectionId: Long, seriesId: Long): Int
    
    fun countByCollectionId(collectionId: Long): Int
    
    @Query("SELECT cs FROM TvCollectionSeries cs WHERE cs.collection.id = :collectionId ORDER BY cs.addedAt DESC")
    fun findTop4ByCollectionIdOrderByAddedAtDesc(collectionId: Long, pageable: Pageable): List<TvCollectionSeries>
    
    @Query("SELECT cs.collection FROM TvCollectionSeries cs WHERE cs.seriesId = :seriesId AND cs.collection.user.id = :userId")
    fun findCollectionsBySeriesIdAndUserId(seriesId: Long, userId: Long): List<TvCollection>
}
