package com.emirhankarci.moviebackend.chat

import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class ChatService(
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
    private val aiService: AiService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ChatService::class.java)
        private const val DAILY_MESSAGE_LIMIT = 5
        private const val CONTEXT_MESSAGE_COUNT = 10
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
        val aiResponse = when (val result = aiService.generateResponse(context)) {
            is AiResult.Success -> result.data
            is AiResult.Error -> {
                logger.error("AI service error for user {}: {}", username, result.message)
                return ChatResult.Error(result.message, ChatErrorCode.AI_ERROR)
            }
        }

        // Save AI response
        val assistantMessage = chatMessageRepository.save(
            ChatMessage(
                user = user,
                content = aiResponse,
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
