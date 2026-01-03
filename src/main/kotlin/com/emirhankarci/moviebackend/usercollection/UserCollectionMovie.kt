package com.emirhankarci.moviebackend.usercollection

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_collection_movies")
data class UserCollectionMovie(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    val collection: UserCollection,

    @Column(name = "movie_id", nullable = false)
    val movieId: Long,

    @Column(name = "movie_title", nullable = false)
    val movieTitle: String,

    @Column(name = "poster_path")
    val posterPath: String? = null,

    @Column(name = "added_at", updatable = false)
    val addedAt: LocalDateTime = LocalDateTime.now()
)
