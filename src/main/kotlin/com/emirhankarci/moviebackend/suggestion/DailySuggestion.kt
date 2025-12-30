package com.emirhankarci.moviebackend.suggestion

import com.emirhankarci.moviebackend.user.User
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "daily_suggestions")
data class DailySuggestion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "movie_ids", nullable = false)
    val movieIds: String,  // "155,27205,157336,680"

    @Column(name = "suggestion_date", nullable = false)
    val suggestionDate: LocalDate,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
