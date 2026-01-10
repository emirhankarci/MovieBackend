package com.emirhankarci.moviebackend.tmdb.dto

/**
 * Pagination metadata for actor filmography and TV credits endpoints.
 */
data class PaginationInfo(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
) {
    companion object {
        /**
         * Calculate pagination info from total items count, page number, and limit.
         * @param totalItems Total number of items
         * @param page Current page number (1-indexed)
         * @param limit Items per page
         */
        fun calculate(totalItems: Int, page: Int, limit: Int): PaginationInfo {
            val effectivePage = maxOf(1, page)
            val effectiveLimit = maxOf(1, limit)
            val totalPages = if (totalItems == 0) 0 else (totalItems + effectiveLimit - 1) / effectiveLimit
            
            return PaginationInfo(
                currentPage = effectivePage,
                totalPages = totalPages,
                totalItems = totalItems,
                hasNextPage = effectivePage < totalPages,
                hasPreviousPage = effectivePage > 1
            )
        }
    }
}

/**
 * Extension function to paginate a list.
 * @param page Page number (1-indexed)
 * @param limit Items per page
 * @return Sublist for the requested page
 */
fun <T> List<T>.paginate(page: Int, limit: Int): List<T> {
    val effectivePage = maxOf(1, page)
    val effectiveLimit = maxOf(1, limit)
    val startIndex = (effectivePage - 1) * effectiveLimit
    
    if (startIndex >= this.size) return emptyList()
    
    val endIndex = minOf(startIndex + effectiveLimit, this.size)
    return this.subList(startIndex, endIndex)
}
