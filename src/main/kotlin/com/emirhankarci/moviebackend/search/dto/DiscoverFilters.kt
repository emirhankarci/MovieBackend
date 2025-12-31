package com.emirhankarci.moviebackend.search.dto

import java.time.LocalDateTime

data class DiscoverFilters(
    val genre: String? = null,
    val minRating: Double? = null,
    val maxRating: Double? = null,
    val year: Int? = null
) {
    fun toJson(): String {
        val parts = mutableListOf<String>()
        genre?.let { parts.add("\"genre\":\"$it\"") }
        minRating?.let { parts.add("\"minRating\":$it") }
        maxRating?.let { parts.add("\"maxRating\":$it") }
        year?.let { parts.add("\"year\":$it") }
        return "{${parts.joinToString(",")}}"
    }
    
    fun isEmpty(): Boolean = genre == null && minRating == null && maxRating == null && year == null
}

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
