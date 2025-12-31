package com.emirhankarci.moviebackend.preferences

import com.emirhankarci.moviebackend.user.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_preferences")
data class UserPreferences(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "genres", nullable = false)
    val genres: String,  // Comma-separated: "ACTION,SCIFI,DRAMA"

    @Column(name = "preferred_era", nullable = false)
    val preferredEra: String,  // Single value: "MODERN"

    @Column(name = "moods", nullable = false)
    val moods: String,  // Comma-separated: "MIND_BENDING,ADRENALINE"

    @Column(name = "favorite_movie_ids", nullable = false)
    val favoriteMovieIds: String,  // Comma-separated TMDB IDs: "157336,155,680"

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
