package com.emirhankarci.moviebackend.tvcollection

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "tv_collection_series")
data class TvCollectionSeries(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    val collection: TvCollection,

    @Column(name = "series_id", nullable = false)
    val seriesId: Long,

    @Column(name = "series_name", nullable = false)
    val seriesName: String,

    @Column(name = "poster_path")
    val posterPath: String? = null,

    @Column(name = "added_at", updatable = false)
    val addedAt: LocalDateTime = LocalDateTime.now()
)
