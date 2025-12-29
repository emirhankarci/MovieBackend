package com.emirhankarci.moviebackend.chat

import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class ChatService(
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
    private val aiService: AiService,
    private val tmdbService: TmdbService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ChatService::class.java)
        private const val DAILY_MESSAGE_LIMIT = 5
        private val objectMapper = jacksonObjectMapper()
    }

    @Transactional
    fun sendMessage(username: String, message: String): ChatResult<ChatResponse> {
        val user = userRepository.findByUsername(username)
            ?: return ChatResult.Error("User not found", ChatErrorCode.USER_NOT_FOUND)

        val userId = user.id ?: return ChatResult.Error("User ID not found", ChatErrorCode.USER_NOT_FOUND)

        // Check daily limit
        val todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay()
        val messageCountToday = chatMessageRepository.countUserMessagesTodayByUserId(userId, todayStart)
        
        if (messageCountToday >= DAILY_MESSAGE_LIMIT) {
            logger.warn("User {} exceeded daily message limit", username)
            return ChatResult.Error(
                "Daily message limit reached. Upgrade to Premium for unlimited access.",
                ChatErrorCode.LIMIT_EXCEEDED
            )
        }

        // Save user message
        val userMessage = chatMessageRepository.save(
            ChatMessage(
                user = user,
                content = message,
                role = MessageRole.USER
            )
        )
        logger.info("User message saved for user {}", username)

        // Get conversation context (last 10 messages)
        val context = chatMessageRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
            .reversed() // Convert to chronological order

        // Call AI service
        val aiResponseRaw = when (val result = aiService.generateResponse(context)) {
            is AiResult.Success -> result.data
            is AiResult.Error -> {
                logger.error("AI service error for user {}: {}", username, result.message)
                return ChatResult.Error(result.message, ChatErrorCode.AI_ERROR)
            }
        }

        // Enrich AI response with TMDB data
        val enrichedResponse = enrichWithTmdbData(aiResponseRaw)

        // Save AI response (enriched version)
        val assistantMessage = chatMessageRepository.save(
            ChatMessage(
                user = user,
                content = enrichedResponse,
                role = MessageRole.ASSISTANT
            )
        )
        logger.info("AI response saved for user {}", username)

        val remainingQuota = DAILY_MESSAGE_LIMIT - (messageCountToday.toInt() + 1)

        return ChatResult.Success(
            ChatResponse(
                userMessage = userMessage.toResponse(),
                aiResponse = assistantMessage.toResponse(),
                remainingQuota = remainingQuota.coerceAtLeast(0)
            )
        )
    }

    private fun enrichWithTmdbData(aiResponse: String): String {
        return try {
            // Clean markdown code blocks if present
            val cleanedResponse = aiResponse
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            // Parse AI response
            val aiData = objectMapper.readValue(cleanedResponse, AiResponseData::class.java)
            
            // If movieTitle exists, search TMDB
            val movieData = aiData.movieTitle?.let { title ->
                tmdbService.searchMovie(title)
            }

            // Build enriched response
            val enrichedData = EnrichedAiResponse(
                preMessage = aiData.preMessage,
                movieData = movieData,
                postMessage = aiData.postMessage
            )

            objectMapper.writeValueAsString(enrichedData)
        } catch (e: Exception) {
            logger.warn("Failed to enrich AI response with TMDB data: {}", e.message)
            // Return original response if parsing fails
            aiResponse
        }
    }

    fun getConversationHistory(username: String): ChatResult<List<ChatMessageResponse>> {
        val user = userRepository.findByUsername(username)
            ?: return ChatResult.Error("User not found", ChatErrorCode.USER_NOT_FOUND)

        val userId = user.id ?: return ChatResult.Error("User ID not found", ChatErrorCode.USER_NOT_FOUND)

        val messages = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(userId)
            .map { it.toResponse() }

        logger.debug("Returning {} messages for user {}", messages.size, username)
        return ChatResult.Success(messages)
    }

    fun getRemainingQuota(username: String): ChatResult<QuotaResponse> {
        val user = userRepository.findByUsername(username)
            ?: return ChatResult.Error("User not found", ChatErrorCode.USER_NOT_FOUND)

        val userId = user.id ?: return ChatResult.Error("User ID not found", ChatErrorCode.USER_NOT_FOUND)

        val todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay()
        val used = chatMessageRepository.countUserMessagesTodayByUserId(userId, todayStart).toInt()
        val remaining = (DAILY_MESSAGE_LIMIT - used).coerceAtLeast(0)

        logger.debug("Quota for user {}: used={}, remaining={}", username, used, remaining)
        return ChatResult.Success(
            QuotaResponse(
                used = used,
                limit = DAILY_MESSAGE_LIMIT,
                remaining = remaining
            )
        )
    }

    private fun ChatMessage.toResponse() = ChatMessageResponse(
        id = this.id!!,
        content = this.content,
        role = this.role,
        createdAt = this.createdAt
    )
}

// AI Response parsing models
data class AiResponseData(
    val preMessage: String,
    val movieTitle: String?,
    val postMessage: String
)

// Enriched response with TMDB data
data class EnrichedAiResponse(
    val preMessage: String,
    val movieData: MovieData?,
    val postMessage: String
)
