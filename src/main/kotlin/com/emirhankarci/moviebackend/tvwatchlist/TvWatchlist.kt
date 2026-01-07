package com.emirhankarci.moviebackend.tvwatchlist

import com.emirhankarci.moviebackend.user.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "tv_watchlist")
data class TvWatchlist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "series_id", nullable = false)
    val seriesId: Long,

    @Column(name = "series_name", nullable = false)
    val seriesName: String,

    @Column(name = "poster_path")
    val posterPath: String? = null,

    @Column(name = "vote_average", precision = 3, scale = 1)
    val voteAverage: BigDecimal? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
