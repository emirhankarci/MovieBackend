package com.emirhankarci.moviebackend.search

import com.emirhankarci.moviebackend.search.dto.DiscoverFilters
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class SearchHistoryService(
    private val searchHistoryRepository: SearchHistoryRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SearchHistoryService::class.java)
    }

    @Async
    fun recordSearch(userId: Long, query: String, resultCount: Int) {
        try {
            val history = SearchHistory(
                userId = userId,
                searchType = SearchType.SEARCH,
                query = query,
                resultCount = resultCount
            )
            searchHistoryRepository.save(history)
            logger.debug("Recorded search history for user {}: query='{}', results={}", 
                userId, query, resultCount)
        } catch (e: Exception) {
            logger.error("Failed to record search history for user {}: {}", userId, e.message)
            // Don't throw - search history failure shouldn't block the response
        }
    }

    @Async
    fun recordDiscover(userId: Long, filters: DiscoverFilters, resultCount: Int) {
        try {
            val history = SearchHistory(
                userId = userId,
                searchType = SearchType.DISCOVER,
                filters = filters.toJson(),
                resultCount = resultCount
            )
            searchHistoryRepository.save(history)
            logger.debug("Recorded discover history for user {}: filters={}, results={}", 
                userId, filters, resultCount)
        } catch (e: Exception) {
            logger.error("Failed to record discover history for user {}: {}", userId, e.message)
            // Don't throw - search history failure shouldn't block the response
        }
    }

    fun getUserSearchHistory(userId: Long): List<SearchHistory> {
        return searchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
    }
}
