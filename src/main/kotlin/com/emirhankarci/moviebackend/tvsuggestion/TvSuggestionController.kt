package com.emirhankarci.moviebackend.tvsuggestion

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tv-suggestions")
class TvSuggestionController(
    private val dailyTvSuggestionService: DailyTvSuggestionService
) {

    @GetMapping("/daily")
    fun getDailyTvSuggestions(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = dailyTvSuggestionService.getDailyTvSuggestions(username)) {
            is TvSuggestionResult.Success -> ResponseEntity.ok(result.data)
            is TvSuggestionResult.Error -> {
                val status = when (result.code) {
                    TvSuggestionErrorCode.USER_NOT_FOUND -> 404
                    TvSuggestionErrorCode.VALIDATION_ERROR -> 422
                    else -> 500
                }
                ResponseEntity.status(status).body(
                    mapOf(
                        "error" to result.code.name,
                        "message" to result.message
                    )
                )
            }
        }
    }
}
