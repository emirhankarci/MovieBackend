package com.emirhankarci.moviebackend.recommendation

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/recommendations")
class RecommendationController(
    private val recommendationService: RecommendationService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RecommendationController::class.java)
    }

    /**
     * Get personalized movie recommendations for the authenticated user
     * Based on a randomly selected movie from their watched list
     */
    @GetMapping("/for-you")
    fun getRecommendationsForYou(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(
                RecommendationErrorResponse(
                    error = "UNAUTHORIZED",
                    message = "Oturum açmanız gerekiyor"
                )
            )

        logger.info("Getting recommendations for user: {}", username)

        return when (val result = recommendationService.getRecommendationsForUser(username, limit)) {
            is RecommendationResult.Success -> {
                ResponseEntity.ok(result.response)
            }
            is RecommendationResult.Empty -> {
                ResponseEntity.ok(
                    EmptyRecommendationsResponse(message = result.message)
                )
            }
            is RecommendationResult.Error -> {
                val status = when (result.code) {
                    "USER_NOT_FOUND" -> 404
                    "EXTERNAL_SERVICE_ERROR" -> 503
                    else -> 500
                }
                ResponseEntity.status(status).body(
                    RecommendationErrorResponse(
                        error = result.code,
                        message = result.message
                    )
                )
            }
        }
    }
}
