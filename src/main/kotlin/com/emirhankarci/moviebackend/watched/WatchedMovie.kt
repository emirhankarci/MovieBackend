package com.emirhankarci.moviebackend.watched

import com.emirhankarci.moviebackend.user.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "watched_movies")
data class WatchedMovie(
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

    @Column(name = "watched_at", updatable = false)
    val watchedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "user_rating", precision = 3, scale = 1)
    val userRating: BigDecimal? = null,

    @Column(name = "imdb_rating", precision = 3, scale = 1)
    val imdbRating: BigDecimal? = null
)
