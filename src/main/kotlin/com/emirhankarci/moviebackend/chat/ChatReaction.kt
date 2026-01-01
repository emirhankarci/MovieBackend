package com.emirhankarci.moviebackend.chat

import com.emirhankarci.moviebackend.user.User
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class ReactionType {
    LIKE, DISLIKE
}

@Entity
@Table(
    name = "chat_reactions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "message_id"])]
)
data class ChatReaction(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "message_id", nullable = false)
    val messageId: Long,

    @Column(name = "movie_id")
    val movieId: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val reaction: ReactionType,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
