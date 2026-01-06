package com.emirhankarci.moviebackend.chat

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

// Request DTOs
data class SendMessageRequest(
    @field:NotBlank(message = "Message cannot be empty")
    val message: String
)

// Response DTOs
data class ChatResponse(
    val userMessage: ChatMessageResponse,
    val aiResponse: ChatMessageResponse,
    val remainingQuota: Int
)

data class ChatMessageResponse(
    val id: Long,
    val content: String,
    val role: MessageRole,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime
)

data class QuotaResponse(
    val used: Int,
    val limit: Int,
    val remaining: Int
)

data class ChatErrorResponse(
    val error: String,
    val code: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now()
)

// Result types
sealed class ChatResult<out T> {
    data class Success<T>(val data: T) : ChatResult<T>()
    data class Error(val message: String, val code: ChatErrorCode) : ChatResult<Nothing>()
}

enum class ChatErrorCode {
    USER_NOT_FOUND,
    LIMIT_EXCEEDED,
    AI_ERROR,
    VALIDATION_ERROR,
    INTERNAL_ERROR
}
