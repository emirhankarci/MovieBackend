package com.emirhankarci.moviebackend.search

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SearchHistoryRepository : JpaRepository<SearchHistory, Long> {
    
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<SearchHistory>
    
    fun findByUserIdAndSearchTypeOrderByCreatedAtDesc(
        userId: Long, 
        searchType: SearchType
    ): List<SearchHistory>
    
    fun findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
        userId: Long, 
        after: LocalDateTime
    ): List<SearchHistory>
}
