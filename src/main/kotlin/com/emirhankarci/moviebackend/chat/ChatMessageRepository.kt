package com.emirhankarci.moviebackend.chat

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {

    fun findByUserIdOrderByCreatedAtAsc(userId: Long): List<ChatMessage>

    fun findTop10ByUserIdOrderByCreatedAtDesc(userId: Long): List<ChatMessage>

    @Query("""
        SELECT COUNT(m) FROM ChatMessage m 
        WHERE m.user.id = :userId 
        AND m.role = 'USER' 
        AND m.createdAt >= :startOfDay
    """)
    fun countUserMessagesTodayByUserId(
        @Param("userId") userId: Long,
        @Param("startOfDay") startOfDay: LocalDateTime
    ): Long
}
