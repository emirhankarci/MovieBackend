package com.emirhankarci.moviebackend.movie

import com.emirhankarci.moviebackend.user.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "watchlist")
data class Watchlist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "movie_id", nullable = false)
    val movieId: Long,

    @Column(name = "movie_title", nullable = false)
    val movieTitle: String,

    @Column(name = "poster_path")
    val posterPath: String? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
