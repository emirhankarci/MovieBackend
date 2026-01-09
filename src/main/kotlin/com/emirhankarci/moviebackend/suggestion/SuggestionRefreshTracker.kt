package com.emirhankarci.moviebackend.suggestion

import com.emirhankarci.moviebackend.user.User
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "suggestion_refresh_tracker")
data class SuggestionRefreshTracker(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "refresh_date", nullable = false)
    val refreshDate: LocalDate,

    @Column(name = "refresh_count", nullable = false)
    var refreshCount: Int = 0
)
