package com.emirhankarci.moviebackend.suggestion

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SuggestionResponseParser(
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SuggestionResponseParser::class.java)
    }

    fun parseSuggestionResponse(jsonResponse: String): List<String> {
        return try {
            // Clean up response - remove markdown code blocks if present
            val cleanJson = jsonResponse
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val response = objectMapper.readValue(cleanJson, SuggestionAiResponse::class.java)
            val titles = response.recommendations.map { it.title }
            
            logger.info("Parsed {} movie titles from AI response", titles.size)
            titles
        } catch (e: Exception) {
            logger.error("Failed to parse AI suggestion response: {}", e.message)
            emptyList()
        }
    }
}

// AI Response models
data class SuggestionAiResponse(
    val recommendations: List<MovieRecommendation>
)

data class MovieRecommendation(
    val title: String
)
