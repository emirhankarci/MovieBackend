package com.emirhankarci.moviebackend.search

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "search_history")
data class SearchHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "search_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val searchType: SearchType,

    @Column(name = "query")
    val query: String? = null,

    @Column(name = "filters", columnDefinition = "TEXT")
    val filters: String? = null, // JSON format

    @Column(name = "result_count", nullable = false)
    val resultCount: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class SearchType {
    SEARCH,
    DISCOVER
}
