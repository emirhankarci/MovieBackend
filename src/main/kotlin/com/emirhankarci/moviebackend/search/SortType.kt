package com.emirhankarci.moviebackend.search

/**
 * Sort tiplerini tanımlayan enum.
 * Her sort tipi için farklı TMDB parametreleri ve QualityFilter thresholdları uygulanır.
 */
enum class SortType {
    RELEASE_DATE,
    VOTE_AVERAGE,
    POPULARITY,
    VOTE_COUNT;

    companion object {
        /**
         * sortBy string'inden SortType çıkarır.
         * Örnek: "release_date.desc" -> RELEASE_DATE
         */
        fun fromSortBy(sortBy: String): SortType {
            val sortBase = sortBy.substringBefore(".")
            return when (sortBase) {
                "release_date" -> RELEASE_DATE
                "vote_average" -> VOTE_AVERAGE
                "popularity" -> POPULARITY
                "vote_count" -> VOTE_COUNT
                else -> POPULARITY // default
            }
        }
    }
}
