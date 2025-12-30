package com.emirhankarci.moviebackend.suggestion

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface DailySuggestionRepository : JpaRepository<DailySuggestion, Long> {
    fun findByUserIdAndSuggestionDate(userId: Long, suggestionDate: LocalDate): Optional<DailySuggestion>
}
