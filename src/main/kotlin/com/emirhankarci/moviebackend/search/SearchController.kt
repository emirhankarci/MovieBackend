package com.emirhankarci.moviebackend.search

import com.emirhankarci.moviebackend.search.dto.*
import com.emirhankarci.moviebackend.user.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/movies")
class SearchController(
    private val movieSearchService: MovieSearchService,
    private val searchHistoryService: SearchHistoryService,
    private val userRepository: UserRepository
) {

    @GetMapping("/search")
    fun searchMovies(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Any> {
        // Validate query
        if (query.isBlank()) {
            return ResponseEntity.badRequest().body(
                ErrorResponse(
                    error = "INVALID_QUERY",
                    message = "Arama sorgusu boş olamaz"
                )
            )
        }

        val userId = getCurrentUserId()
        
        return when (val result = movieSearchService.searchByName(query, page, size)) {
            is SearchResult.Success -> {
                // Record search history asynchronously
                userId?.let { 
                    searchHistoryService.recordSearch(it, query, result.movies.size) 
                }
                
                ResponseEntity.ok(
                    SearchResponse(
                        movies = result.movies,
                        pagination = result.pagination
                    )
                )
            }
            is SearchResult.Error -> {
                ResponseEntity.status(503).body(
                    ErrorResponse(
                        error = result.code,
                        message = result.message
                    )
                )
            }
        }
    }

    @GetMapping("/discover")
    fun discoverMovies(
        @RequestParam(required = false) genre: String?,
        @RequestParam(required = false) minRating: Double?,
        @RequestParam(required = false) maxRating: Double?,
        @RequestParam(required = false) year: Int?,
        @RequestParam(defaultValue = "popularity.desc") sortBy: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Any> {
        // Validate sortBy parameter
        if (!SortOption.isValid(sortBy)) {
            return ResponseEntity.badRequest().body(
                ErrorResponse(
                    error = "INVALID_SORT_OPTION",
                    message = "Geçersiz sıralama seçeneği: $sortBy. Geçerli seçenekler: ${SortOption.getAllValues().joinToString(", ")}"
                )
            )
        }

        // Validate genre if provided
        if (genre != null && !TmdbGenreMapper.isValidGenre(genre)) {
            return ResponseEntity.badRequest().body(
                ErrorResponse(
                    error = "INVALID_GENRE",
                    message = "Geçersiz tür: $genre. Geçerli türler: ${TmdbGenreMapper.getAllGenres().joinToString(", ")}"
                )
            )
        }

        // Validate rating range
        if (minRating != null && maxRating != null && minRating > maxRating) {
            return ResponseEntity.badRequest().body(
                ErrorResponse(
                    error = "INVALID_RATING_RANGE",
                    message = "Minimum puan maksimum puandan büyük olamaz"
                )
            )
        }

        val filters = DiscoverFilters(
            genre = genre,
            minRating = minRating,
            maxRating = maxRating,
            year = year,
            sortBy = sortBy
        )

        val userId = getCurrentUserId()

        return when (val result = movieSearchService.discoverWithFilters(filters, page, size)) {
            is SearchResult.Success -> {
                // Record discover history asynchronously
                userId?.let { 
                    searchHistoryService.recordDiscover(it, filters, result.movies.size) 
                }
                
                ResponseEntity.ok(
                    SearchResponse(
                        movies = result.movies,
                        pagination = result.pagination
                    )
                )
            }
            is SearchResult.Error -> {
                ResponseEntity.status(503).body(
                    ErrorResponse(
                        error = result.code,
                        message = result.message
                    )
                )
            }
        }
    }

    @GetMapping("/genres")
    fun getAvailableGenres(): ResponseEntity<Any> {
        return ResponseEntity.ok(
            mapOf("genres" to TmdbGenreMapper.getAllGenres())
        )
    }

    private fun getCurrentUserId(): Long? {
        val username = SecurityContextHolder.getContext().authentication?.name ?: return null
        return userRepository.findByUsername(username)?.id
    }
}
