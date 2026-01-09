package com.emirhankarci.moviebackend.tvsuggestion

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TvSuggestionResponseParser(
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvSuggestionResponseParser::class.java)
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

            // Parse as TvSuggestionAiResponse (recommendations format)
            val response = objectMapper.readValue(cleanJson, TvSuggestionAiResponse::class.java)

            if (response.recommendations.isNotEmpty()) {
                val titles = response.recommendations.map { it.title }.filter { it.isNotBlank() }
                logger.info("Parsed {} TV series titles from AI response", titles.size)
                return titles
            }

            logger.warn("No TV series titles found in AI response")
            emptyList()
        } catch (e: Exception) {
            logger.error("Failed to parse AI TV suggestion response: {}", e.message)
            // Try regex extraction as last resort
            extractTitlesFromText(jsonResponse)
        }
    }

    private fun extractTitlesFromText(text: String): List<String> {
        val titles = mutableListOf<String>()

        // Pattern for {"title": "Series Name"}
        val titlePattern = """"title"\s*:\s*"([^"]+)"""".toRegex()
        titlePattern.findAll(text).forEach { match ->
            val title = match.groupValues[1].trim()
            if (title.isNotBlank() && title.length in 2..100) {
                titles.add(title)
            }
        }

        if (titles.isNotEmpty()) {
            logger.info("Extracted {} TV series titles via regex fallback", titles.size)
        }

        return titles
    }
}

// AI Response models for TV series
@JsonIgnoreProperties(ignoreUnknown = true)
data class TvSuggestionAiResponse(
    val recommendations: List<TvSeriesRecommendation> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TvSeriesRecommendation(
    val title: String = ""
)
