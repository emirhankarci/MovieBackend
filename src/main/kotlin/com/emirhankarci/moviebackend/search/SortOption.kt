package com.emirhankarci.moviebackend.search

enum class SortOption(val value: String) {
    POPULARITY_DESC("popularity.desc"),
    POPULARITY_ASC("popularity.asc"),
    RELEASE_DATE_DESC("release_date.desc"),
    RELEASE_DATE_ASC("release_date.asc"),
    VOTE_AVERAGE_DESC("vote_average.desc"),
    VOTE_AVERAGE_ASC("vote_average.asc"),
    VOTE_COUNT_DESC("vote_count.desc");

    companion object {
        fun fromValue(value: String): SortOption? =
            entries.find { it.value == value }

        fun isValid(value: String): Boolean =
            fromValue(value) != null

        fun getAllValues(): List<String> =
            entries.map { it.value }
    }
}
