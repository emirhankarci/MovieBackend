package com.emirhankarci.moviebackend.suggestion

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
            var cleanJson = jsonResponse
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            // Try to extract JSON object if there's extra text
            val jsonStart = cleanJson.indexOf('{')
            val jsonEnd = cleanJson.lastIndexOf('}')
            
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                cleanJson = cleanJson.substring(jsonStart, jsonEnd + 1)
            }

            // First try to parse as SuggestionAiResponse (recommendations format)
            val response = objectMapper.readValue(cleanJson, SuggestionAiResponse::class.java)
            
            if (response.recommendations.isNotEmpty()) {
                val titles = response.recommendations.map { it.title }.filter { it.isNotBlank() }
                logger.info("Parsed {} movie titles from AI response", titles.size)
                return titles
            }
            
            // If recommendations is empty, try to extract movieTitle from chat format
            val chatResponse = objectMapper.readValue(cleanJson, ChatFormatResponse::class.java)
            if (!chatResponse.movieTitle.isNullOrBlank()) {
                logger.info("Extracted movie title from chat format: {}", chatResponse.movieTitle)
                return listOf(chatResponse.movieTitle)
            }
            
            logger.warn("No movie titles found in AI response")
            emptyList()
        } catch (e: Exception) {
            logger.error("Failed to parse AI suggestion response: {}", e.message)
            // Try regex extraction as last resort
            extractTitlesFromText(jsonResponse)
        }
    }
    
    private fun extractTitlesFromText(text: String): List<String> {
        val titles = mutableListOf<String>()
        
        // Pattern for {"title": "Movie Name"}
        val titlePattern = """"title"\s*:\s*"([^"]+)"""".toRegex()
        titlePattern.findAll(text).forEach { match ->
            val title = match.groupValues[1].trim()
            if (title.isNotBlank() && title.length in 2..100) {
                titles.add(title)
            }
        }
        
        if (titles.isNotEmpty()) {
            logger.info("Extracted {} titles via regex fallback", titles.size)
        }
        
        return titles
    }
}

// AI Response models - ignore unknown fields for flexibility
@JsonIgnoreProperties(ignoreUnknown = true)
data class SuggestionAiResponse(
    val recommendations: List<MovieRecommendation> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovieRecommendation(
    val title: String = ""
)

// Chat format response for fallback parsing
@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatFormatResponse(
    val preMessage: String? = null,
    val movieTitle: String? = null,
    val postMessage: String? = null
)
