package com.emirhankarci.moviebackend.chat

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

@Service
class GeminiAiService(
    private val restTemplate: RestTemplate
) : AiService {

    companion object {
        private val logger = LoggerFactory.getLogger(GeminiAiService::class.java)
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        private const val SYSTEM_PROMPT = """
            Sen bir film ve dizi asistanısın. Kullanıcılara film ve dizi önerileri yapabilir,
            filmler hakkında bilgi verebilir, oyuncular ve yönetmenler hakkında konuşabilirsin.
            Türkçe yanıt ver. Kısa ve öz cevaplar ver.
        """
    }

    private val apiKey: String = System.getenv("GEMINI_API_KEY")
        ?: throw IllegalStateException("GEMINI_API_KEY environment variable must be set!")

    override fun generateResponse(conversationContext: List<ChatMessage>): AiResult<String> {
        return try {
            val requestBody = buildRequestBody(conversationContext)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(requestBody, headers)
            val url = "$GEMINI_API_URL?key=$apiKey"

            logger.debug("Calling Gemini API with {} messages in context", conversationContext.size)
            
            val response = restTemplate.postForObject(url, entity, GeminiResponse::class.java)
            
            val text = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            
            if (text != null) {
                logger.info("Gemini API response received successfully")
                AiResult.Success(text)
            } else {
                logger.warn("Gemini API returned empty response")
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

    private fun buildRequestBody(conversationContext: List<ChatMessage>): GeminiRequest {
        val contents = mutableListOf<GeminiContent>()
        
        // Add system instruction as first user message
        contents.add(GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(SYSTEM_PROMPT.trimIndent()))
        ))
        contents.add(GeminiContent(
            role = "model",
            parts = listOf(GeminiPart("Anladım! Film ve dizi konularında size yardımcı olmaya hazırım."))
        ))
        
        // Add conversation history (last 10 messages, reversed to chronological order)
        val recentMessages = conversationContext.takeLast(10)
        recentMessages.forEach { message ->
            contents.add(GeminiContent(
                role = if (message.role == MessageRole.USER) "user" else "model",
                parts = listOf(GeminiPart(message.content))
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
