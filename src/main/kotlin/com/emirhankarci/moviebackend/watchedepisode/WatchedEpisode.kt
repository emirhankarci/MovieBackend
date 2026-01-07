package com.emirhankarci.moviebackend.watchedepisode

import com.emirhankarci.moviebackend.user.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "watched_episodes")
data class WatchedEpisode(
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

    @Column(name = "season_number", nullable = false)
    val seasonNumber: Int,

    @Column(name = "episode_number", nullable = false)
    val episodeNumber: Int,

    @Column(name = "episode_name")
    val episodeName: String? = null,

    @Column(name = "watched_at", updatable = false)
    val watchedAt: LocalDateTime = LocalDateTime.now()
)
