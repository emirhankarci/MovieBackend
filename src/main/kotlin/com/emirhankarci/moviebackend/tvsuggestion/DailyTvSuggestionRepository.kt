package com.emirhankarci.moviebackend.tvsuggestion

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface DailyTvSuggestionRepository : JpaRepository<DailyTvSuggestion, Long> {
    
    fun findByUserIdAndSuggestionDate(userId: Long, suggestionDate: LocalDate): Optional<DailyTvSuggestion>
    
    fun existsByUserIdAndSuggestionDate(userId: Long, suggestionDate: LocalDate): Boolean
}
