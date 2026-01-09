package com.emirhankarci.moviebackend.suggestion

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface SuggestionRefreshTrackerRepository : JpaRepository<SuggestionRefreshTracker, Long> {
    fun findByUserIdAndRefreshDate(userId: Long, refreshDate: LocalDate): Optional<SuggestionRefreshTracker>
}
