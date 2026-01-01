package com.emirhankarci.moviebackend.chat

import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReactionService(
    private val chatReactionRepository: ChatReactionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ReactionService::class.java)
    }

    @Transactional
    fun addReaction(username: String, messageId: Long, request: ReactionRequest): ReactionResult<ReactionResponse> {
        // Validate request
        when (val validation = request.validate()) {
            is ReactionValidationResult.Invalid -> {
                logger.warn("Invalid reaction from user {}: {}", username, validation.message)
                return ReactionResult.Error(validation.message, ReactionErrorCode.INVALID_REACTION)
            }
            is ReactionValidationResult.Valid -> { /* continue */ }
        }

        val user = userRepository.findByUsername(username)
            ?: return ReactionResult.Error("User not found", ReactionErrorCode.USER_NOT_FOUND)

        val userId = user.id ?: return ReactionResult.Error("User ID not found", ReactionErrorCode.USER_NOT_FOUND)

        // Check if message exists
        val messageExists = chatMessageRepository.existsById(messageId)
        if (!messageExists) {
            logger.warn("Message {} not found for reaction from user {}", messageId, username)
            return ReactionResult.Error("Message not found", ReactionErrorCode.MESSAGE_NOT_FOUND)
        }

        val reactionType = request.toReactionType()

        // Check if user already reacted to this message
        val existingReaction = chatReactionRepository.findByUserIdAndMessageId(userId, messageId)

        val savedReaction = if (existingReaction.isPresent) {
            // Update existing reaction
            val updated = existingReaction.get().copy(
                reaction = reactionType,
                movieId = request.movieId ?: existingReaction.get().movieId
            )
            chatReactionRepository.save(updated)
            logger.info("Updated reaction to {} for message {} by user {}", reactionType, messageId, username)
            updated
        } else {
            // Create new reaction
            val newReaction = ChatReaction(
                user = user,
                messageId = messageId,
                movieId = request.movieId,
                reaction = reactionType
            )
            chatReactionRepository.save(newReaction)
            logger.info("Added {} reaction for message {} by user {}", reactionType, messageId, username)
            newReaction
        }

        return ReactionResult.Success(
            ReactionResponse(
                success = true,
                messageId = messageId,
                reaction = savedReaction.reaction.name.lowercase()
            )
        )
    }

    fun getReaction(username: String, messageId: Long): ReactionResult<ReactionResponse?> {
        val user = userRepository.findByUsername(username)
            ?: return ReactionResult.Error("User not found", ReactionErrorCode.USER_NOT_FOUND)

        val userId = user.id ?: return ReactionResult.Error("User ID not found", ReactionErrorCode.USER_NOT_FOUND)

        val reaction = chatReactionRepository.findByUserIdAndMessageId(userId, messageId)

        return if (reaction.isPresent) {
            ReactionResult.Success(
                ReactionResponse(
                    success = true,
                    messageId = messageId,
                    reaction = reaction.get().reaction.name.lowercase()
                )
            )
        } else {
            ReactionResult.Success(null)
        }
    }
}
