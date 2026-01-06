package com.emirhankarci.moviebackend.common

import org.springframework.data.domain.Page

/**
 * Generic pagination response DTO for all paginated endpoints.
 * Provides consistent pagination format across the API.
 */
data class PageResponse<T : Any>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        /**
         * Create PageResponse from Spring Data Page
         */
        fun <T : Any> from(page: Page<T>): PageResponse<T> {
            return PageResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious()
            )
        }

        /**
         * Create PageResponse from Spring Data Page with mapper function
         */
        fun <T : Any, R : Any> from(page: Page<T>, mapper: (T) -> R): PageResponse<R> {
            return PageResponse(
                content = page.content.map(mapper),
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious()
            )
        }

        /**
         * Create empty PageResponse
         */
        fun <T : Any> empty(page: Int = 0, size: Int = 20): PageResponse<T> {
            return PageResponse(
                content = emptyList(),
                page = page,
                size = size,
                totalElements = 0,
                totalPages = 0,
                hasNext = false,
                hasPrevious = false
            )
        }
    }
}
