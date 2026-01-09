package com.emirhankarci.moviebackend.suggestion

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/suggestions")
class DailySuggestionController(
    private val dailySuggestionService: DailySuggestionService,
    private val refreshSuggestionService: RefreshSuggestionService
) {

    @GetMapping("/daily")
    fun getDailySuggestions(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(
                SuggestionErrorResponse("Unauthorized", "UNAUTHORIZED")
            )

        return when (val result = dailySuggestionService.getDailySuggestions(username)) {
            is SuggestionResult.Success -> ResponseEntity.ok(result.data)
            is SuggestionResult.Error -> {
                val statusCode = when (result.code) {
                    SuggestionErrorCode.USER_NOT_FOUND -> 404
                    SuggestionErrorCode.AI_ERROR -> 503
                    SuggestionErrorCode.TMDB_ERROR -> 503
                    SuggestionErrorCode.VALIDATION_ERROR -> 422
                    SuggestionErrorCode.REFRESH_LIMIT_EXCEEDED -> 429
                    SuggestionErrorCode.UNKNOWN -> 500
                }
                ResponseEntity.status(statusCode).body(
                    SuggestionErrorResponse(result.message, result.code.name)
                )
            }
}
    }

    @PostMapping("/refresh")
    fun refreshSuggestions(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(
                SuggestionErrorResponse("Unauthorized", "UNAUTHORIZED")
            )

        return when (val result = refreshSuggestionService.refreshSuggestions(username)) {
            is SuggestionResult.Success -> ResponseEntity.ok(result.data)
            is SuggestionResult.Error -> {
                val statusCode = when (result.code) {
                    SuggestionErrorCode.USER_NOT_FOUND -> 404
                    SuggestionErrorCode.AI_ERROR -> 503
                    SuggestionErrorCode.TMDB_ERROR -> 503
                    SuggestionErrorCode.VALIDATION_ERROR -> 422
                    SuggestionErrorCode.REFRESH_LIMIT_EXCEEDED -> 429
                    SuggestionErrorCode.UNKNOWN -> 500
                }
                ResponseEntity.status(statusCode).body(
                    SuggestionErrorResponse(result.message, result.code.name)
                )
            }
        }
    }

    @GetMapping("/refresh/status")
    fun getRefreshStatus(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(
                SuggestionErrorResponse("Unauthorized", "UNAUTHORIZED")
            )

        return when (val result = refreshSuggestionService.getRefreshStatus(username)) {
            is SuggestionResult.Success -> ResponseEntity.ok(result.data)
            is SuggestionResult.Error -> {
                val statusCode = when (result.code) {
                    SuggestionErrorCode.USER_NOT_FOUND -> 404
                    SuggestionErrorCode.AI_ERROR -> 503
                    SuggestionErrorCode.TMDB_ERROR -> 503
                    SuggestionErrorCode.VALIDATION_ERROR -> 422
                    SuggestionErrorCode.REFRESH_LIMIT_EXCEEDED -> 429
                    SuggestionErrorCode.UNKNOWN -> 500
                }
                ResponseEntity.status(statusCode).body(
                    SuggestionErrorResponse(result.message, result.code.name)
                )
            }
        }
    }
}

data class SuggestionErrorResponse(
    val error: String,
    val code: String
)
