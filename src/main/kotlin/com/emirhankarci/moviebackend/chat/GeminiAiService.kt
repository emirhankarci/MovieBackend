package com.emirhankarci.moviebackend.chat

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import tools.jackson.module.kotlin.jacksonObjectMapper

@Service
class GeminiAiService(
    private val restTemplate: RestTemplate
) : AiService {

    companion object {
        private val logger = LoggerFactory.getLogger(GeminiAiService::class.java)
        private val objectMapper = jacksonObjectMapper()
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        private const val MAX_RETRY_ATTEMPTS = 2
        
        private const val SYSTEM_PROMPT = """
Sen bir film öneri asistanısın. ANA GÖREVİN FİLM ÖNERMEKTİR.

⚠️ EN ÖNEMLİ KURAL:
Kullanıcı ne derse desin, HER ZAMAN bir film öner. movieTitle ASLA null OLMAMALI.
Selamlama bile olsa, bir film önerisiyle karşılık ver.

ZORUNLU JSON FORMATI (TEK SATIR):
{"preMessage":"kısa giriş","movieTitle":"İNGİLİZCE FİLM ADI","postMessage":"film hakkında bilgi"}

⚠️ movieTitle KURALLARI:
- movieTitle HER ZAMAN dolu olmalı, ASLA null yazma
- movieTitle İNGİLİZCE orijinal film adı olmalı (örn: "The Godfather", "Inception")
- Türkçe film adı YAZMA, İngilizce yaz

ÖRNEKLER:

Kullanıcı: "Merhaba"
{"preMessage":"Merhaba! Sana hemen bir film önereyim:","movieTitle":"The Shawshank Redemption","postMessage":"Tüm zamanların en iyi filmi olarak kabul edilir. Umut ve dostluk üzerine."}

Kullanıcı: "Aksiyon filmi öner"
{"preMessage":"Harika bir aksiyon filmi:","movieTitle":"The Dark Knight","postMessage":"Heath Ledger'ın efsane Joker performansı. Süper kahraman filmlerinin zirvesi."}

Kullanıcı: "Ne izlesem?"
{"preMessage":"Bugün için önerim:","movieTitle":"Inception","postMessage":"Christopher Nolan'ın zihin büken başyapıtı. Rüyalar içinde rüyalar."}

Kullanıcı: "Teşekkürler"
{"preMessage":"Rica ederim! Bir film daha önereyim:","movieTitle":"Interstellar","postMessage":"Uzay ve zaman üzerine epik bir yolculuk. Görsel efektleri muhteşem."}

Kullanıcı: "Interstellar nasıl?"
{"preMessage":"Interstellar hakkında:","movieTitle":"Interstellar","postMessage":"2014 yapımı bilim kurgu başyapıtı. Matthew McConaughey başrolde."}

⚠️ UYARILAR:
1. Cevap { ile başlamalı } ile bitmeli
2. Markdown KULLANMA, code block KULLANMA
3. movieTitle ASLA null olmasın, her zaman bir film adı yaz
4. Türkçe yanıt ver ama film adı İNGİLİZCE olsun
"""

        private const val RETRY_PROMPT = """
ÖNCEKİ CEVABIN HATALI! 

TEKRAR: movieTitle ASLA null OLMAMALI. Her zaman bir film öner.
Format: {"preMessage":"...","movieTitle":"İNGİLİZCE FİLM ADI","postMessage":"..."}

Örnek: {"preMessage":"İşte önerim:","movieTitle":"The Matrix","postMessage":"Bilim kurgu klasiği."}
"""
    }

    private val apiKey: String = System.getenv("GEMINI_API_KEY")
        ?: throw IllegalStateException("GEMINI_API_KEY environment variable must be set!")

    override fun generateResponse(conversationContext: List<ChatMessage>): AiResult<String> {
        var lastResponse: String? = null
        
        for (attempt in 1..MAX_RETRY_ATTEMPTS) {
            val result = callGeminiApi(conversationContext, isRetry = attempt > 1)
            
            when (result) {
                is AiResult.Success -> {
                    val response = result.data
                    lastResponse = response
                    
                    // Validate JSON format
                    val validatedResponse = validateAndCleanResponse(response)
                    if (validatedResponse != null) {
                        logger.info("Valid JSON response received on attempt {}", attempt)
                        return AiResult.Success(validatedResponse)
                    }
                    
                    logger.warn("Invalid JSON response on attempt {}: {}", attempt, response.take(100))
                }
                is AiResult.Error -> {
                    logger.error("API error on attempt {}: {}", attempt, result.message)
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        return result
                    }
                }
            }
        }
        
        // All retries failed - create fallback response
        logger.warn("All retry attempts failed, creating fallback response")
        val fallbackResponse = createFallbackResponse(lastResponse)
        return AiResult.Success(fallbackResponse)
    }

    private fun callGeminiApi(conversationContext: List<ChatMessage>, isRetry: Boolean): AiResult<String> {
        return try {
            val requestBody = buildRequestBody(conversationContext, isRetry)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(requestBody, headers)
            val url = "$GEMINI_API_URL?key=$apiKey"

            logger.debug("Calling Gemini API (retry={})", isRetry)
            
            val response = restTemplate.postForObject(url, entity, GeminiResponse::class.java)
            val text = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            
            if (text != null) {
                AiResult.Success(text)
            } else {
                AiResult.Error("AI returned empty response", AiErrorCode.INVALID_RESPONSE)
            }
        } catch (e: RestClientException) {
            logger.error("Gemini API call failed: {}", e.message)
            when {
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    AiResult.Error("AI service timed out", AiErrorCode.TIMEOUT)
                e.message?.contains("429") == true -> 
                    AiResult.Error("AI service rate limited", AiErrorCode.RATE_LIMITED)
                else -> 
                    AiResult.Error("AI service error: ${e.message}", AiErrorCode.API_ERROR)
            }
        } catch (e: Exception) {
            logger.error("Unexpected error calling Gemini API: {}", e.message)
            AiResult.Error("Unexpected AI error", AiErrorCode.API_ERROR)
        }
    }


    private fun validateAndCleanResponse(response: String): String? {
        return try {
            // Clean the response
            var cleaned = response
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            // Try to extract JSON if there's extra text
            val jsonStart = cleaned.indexOf('{')
            val jsonEnd = cleaned.lastIndexOf('}')
            
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
            }
            
            // Validate by parsing
            val parsed = objectMapper.readTree(cleaned)
            
            // Check required fields exist
            if (parsed.has("preMessage") && parsed.has("postMessage")) {
                cleaned
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug("JSON validation failed: {}", e.message)
            null
        }
    }

    private fun createFallbackResponse(lastResponse: String?): String {
        // Try to extract movie title from plain text response
        var movieTitle = extractMovieTitleFromText(lastResponse)
        
        // If no movie title found, use a default popular movie
        if (movieTitle == null) {
            val defaultMovies = listOf(
                "The Shawshank Redemption",
                "The Godfather", 
                "The Dark Knight",
                "Pulp Fiction",
                "Forrest Gump",
                "Inception",
                "The Matrix",
                "Interstellar"
            )
            movieTitle = defaultMovies.random()
            logger.info("Using default movie recommendation: {}", movieTitle)
        }
        
        val fallback = if (lastResponse != null) {
            // Use the plain text as preMessage
            val cleanedText = lastResponse
                .replace("```json", "")
                .replace("```", "")
                .replace("{", "")
                .replace("}", "")
                .replace("\"", "")
                .trim()
                .take(500) // Limit length
            
            mapOf(
                "preMessage" to cleanedText,
                "movieTitle" to movieTitle,
                "postMessage" to "Bu filmi mutlaka izlemeni öneririm!"
            )
        } else {
            mapOf(
                "preMessage" to "Sana harika bir film öneriyorum:",
                "movieTitle" to movieTitle,
                "postMessage" to "Bu filmi mutlaka izlemeni öneririm!"
            )
        }
        
        return objectMapper.writeValueAsString(fallback)
    }

    private fun extractMovieTitleFromText(text: String?): String? {
        if (text == null) return null
        
        // Common patterns to find movie titles
        val patterns = listOf(
            // "Film Adı" pattern (quoted)
            """"([^"]+)"""".toRegex(),
            // 'Film Adı' pattern (single quoted)
            """'([^']+)'""".toRegex(),
            // "movieTitle": "Film Adı" pattern
            """"movieTitle"\s*:\s*"([^"]+)"""".toRegex(),
            // Common movie title indicators
            """(?:öneri(?:yo)?r?u?m|tavsiye ederim|izlemeni öneririm)[:\s]+([A-Z][^.!?\n]+)""".toRegex(RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val title = match.groupValues[1].trim()
                // Validate it looks like a movie title (not too long, not empty)
                if (title.length in 2..100 && !title.contains("\n")) {
                    logger.debug("Extracted movie title from text: {}", title)
                    return title
                }
            }
        }
        
        return null
    }

    override fun generateSuggestions(prompt: String): AiResult<String> {
        var lastResponse: String? = null
        
        for (attempt in 1..MAX_RETRY_ATTEMPTS) {
            val result = callGeminiApiForSuggestions(prompt, isRetry = attempt > 1)
            
            when (result) {
                is AiResult.Success -> {
                    val response = result.data
                    lastResponse = response
                    
                    // Validate JSON format for suggestions
                    val validatedResponse = validateSuggestionResponse(response)
                    if (validatedResponse != null) {
                        logger.info("Valid suggestion JSON response received on attempt {}", attempt)
                        return AiResult.Success(validatedResponse)
                    }
                    
                    logger.warn("Invalid suggestion JSON response on attempt {}: {}", attempt, response.take(100))
                }
                is AiResult.Error -> {
                    logger.error("API error on attempt {}: {}", attempt, result.message)
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        return result
                    }
                }
            }
        }
        
        // All retries failed - try to extract titles from last response
        logger.warn("All suggestion retry attempts failed")
        return if (lastResponse != null) {
            AiResult.Success(lastResponse)
        } else {
            AiResult.Error("Could not generate suggestions", AiErrorCode.INVALID_RESPONSE)
        }
    }

    private fun callGeminiApiForSuggestions(prompt: String, isRetry: Boolean): AiResult<String> {
        return try {
            val contents = mutableListOf<GeminiContent>()
            
            // Add the suggestion prompt
            contents.add(GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(prompt))
            ))
            
            // Add retry instruction if needed
            if (isRetry) {
                contents.add(GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart("""
ÖNCEKİ CEVABIN HATALI! Sadece JSON döndür.
Format: {"recommendations": [{"title": "Film 1"}, {"title": "Film 2"}, {"title": "Film 3"}, {"title": "Film 4"}]}
Markdown code block KULLANMA. Sadece JSON.
                    """.trimIndent()))
                ))
            }
            
            val requestBody = GeminiRequest(contents = contents)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(requestBody, headers)
            val url = "$GEMINI_API_URL?key=$apiKey"

            logger.debug("Calling Gemini API for suggestions (retry={})", isRetry)
            
            val response = restTemplate.postForObject(url, entity, GeminiResponse::class.java)
            val text = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            
            if (text != null) {
                AiResult.Success(text)
            } else {
                AiResult.Error("AI returned empty response", AiErrorCode.INVALID_RESPONSE)
            }
        } catch (e: RestClientException) {
            logger.error("Gemini API call failed: {}", e.message)
            AiResult.Error("AI service error: ${e.message}", AiErrorCode.API_ERROR)
        } catch (e: Exception) {
            logger.error("Unexpected error calling Gemini API: {}", e.message)
            AiResult.Error("Unexpected AI error", AiErrorCode.API_ERROR)
        }
    }

    private fun validateSuggestionResponse(response: String): String? {
        return try {
            // Clean the response
            var cleaned = response
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            // Try to extract JSON if there's extra text
            val jsonStart = cleaned.indexOf('{')
            val jsonEnd = cleaned.lastIndexOf('}')
            
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
            }
            
            // Validate by parsing
            val parsed = objectMapper.readTree(cleaned)
            
            // Check if it has recommendations array
            if (parsed.has("recommendations") && parsed.get("recommendations").isArray) {
                cleaned
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug("Suggestion JSON validation failed: {}", e.message)
            null
        }
    }

    private fun buildRequestBody(conversationContext: List<ChatMessage>, isRetry: Boolean): GeminiRequest {
        val contents = mutableListOf<GeminiContent>()
        
        // Add system instruction as first user message
        contents.add(GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(SYSTEM_PROMPT.trimIndent()))
        ))
        contents.add(GeminiContent(
            role = "model",
            parts = listOf(GeminiPart("""{"preMessage":"Anladım!","movieTitle":null,"postMessage":"Film önerisi için hazırım."}"""))
        ))
        
        // Add conversation history
        val recentMessages = conversationContext.takeLast(10)
        recentMessages.forEach { message ->
            contents.add(GeminiContent(
                role = if (message.role == MessageRole.USER) "user" else "model",
                parts = listOf(GeminiPart(message.content))
            ))
        }
        
        // Add retry prompt if this is a retry attempt
        if (isRetry) {
            contents.add(GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(RETRY_PROMPT.trimIndent()))
            ))
        }
        
        return GeminiRequest(contents = contents)
    }
}

// Gemini API Request/Response models
data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)
