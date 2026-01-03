package com.emirhankarci.moviebackend.chat

interface AiService {
    fun generateResponse(conversationContext: List<ChatMessage>, userContext: String? = null): AiResult<String>
    
    /**
     * Generate movie suggestions based on a prompt.
     * Returns raw JSON in recommendations format.
     */
    fun generateSuggestions(prompt: String): AiResult<String>
}

sealed class AiResult<out T> {
    data class Success<T>(val data: T) : AiResult<T>()
    data class Error(val message: String, val code: AiErrorCode) : AiResult<Nothing>()
}

enum class AiErrorCode {
    API_ERROR,
    TIMEOUT,
    RATE_LIMITED,
    INVALID_RESPONSE
}
