package com.emirhankarci.moviebackend.tvrating

import com.emirhankarci.moviebackend.user.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "tv_series_ratings")
data class TvSeriesRating(
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

    @Column(name = "rating", precision = 3, scale = 1, nullable = false)
    val rating: BigDecimal,

    @Column(name = "rated_at")
    val ratedAt: LocalDateTime = LocalDateTime.now()
)
