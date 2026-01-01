package com.emirhankarci.moviebackend.chat

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ChatReactionRepository : JpaRepository<ChatReaction, UUID> {
    fun findByUserIdAndMessageId(userId: Long, messageId: Long): Optional<ChatReaction>
    fun findByUserId(userId: Long): List<ChatReaction>
    fun findByMessageId(messageId: Long): List<ChatReaction>
}
