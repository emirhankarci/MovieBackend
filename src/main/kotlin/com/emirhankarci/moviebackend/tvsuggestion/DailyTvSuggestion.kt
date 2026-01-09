package com.emirhankarci.moviebackend.tvsuggestion

import com.emirhankarci.moviebackend.user.User
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "daily_tv_suggestions")
data class DailyTvSuggestion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "tv_series_ids", nullable = false)
    val tvSeriesIds: String,  // Comma-separated TMDB IDs: "1396,1399,66732,93405"

    @Column(name = "suggestion_date", nullable = false)
    val suggestionDate: LocalDate,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
