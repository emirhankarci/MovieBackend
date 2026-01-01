package com.emirhankarci.moviebackend.search

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Sort tipine göre TMDB API parametrelerini oluşturan builder.
 * Her sort tipi için gerekli ek parametreleri ekler.
 */
object SortParameterBuilder {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    /**
     * sortBy string'ine göre TMDB API için ek parametreler oluşturur.
     * 
     * @param sortBy Sıralama parametresi (örn: "release_date.desc")
     * @return Ek TMDB parametreleri map'i
     */
    fun buildParameters(sortBy: String): Map<String, String> {
        return when (sortBy) {
            "release_date.desc" -> mapOf(
                "release_date.lte" to LocalDate.now().format(dateFormatter)
            )
            "release_date.asc" -> mapOf(
                "release_date.gte" to "1990-01-01"
            )
            "vote_average.desc" -> mapOf(
                "vote_count.gte" to "500"
            )
            "vote_average.asc" -> mapOf(
                "vote_count.gte" to "100"
            )
            // popularity.* ve vote_count.* için ek parametre gerekmez
            else -> emptyMap()
        }
    }
}
