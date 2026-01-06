package com.emirhankarci.moviebackend.chat

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class ReactionRequest(
    @field:NotBlank(message = "Reaction cannot be empty")
    @field:Pattern(regexp = "(?i)^(like|dislike)$", message = "Reaction must be 'like' or 'dislike'")
    val reaction: String,
    val movieId: Int? = null
) {
    fun toReactionType(): ReactionType {
        return when (reaction.lowercase().trim()) {
            "like" -> ReactionType.LIKE
            "dislike" -> ReactionType.DISLIKE
            else -> throw IllegalArgumentException("Invalid reaction type")
        }
    }
}

data class ReactionResponse(
    val success: Boolean,
    val messageId: Long,
    val reaction: String
)

// Result type for reactions
sealed class ReactionResult<out T> {
    data class Success<T>(val data: T) : ReactionResult<T>()
    data class Error(val message: String, val code: ReactionErrorCode) : ReactionResult<Nothing>()
}

enum class ReactionErrorCode {
    USER_NOT_FOUND,
    MESSAGE_NOT_FOUND,
    INVALID_REACTION,
    INTERNAL_ERROR
}
