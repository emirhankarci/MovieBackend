package com.emirhankarci.moviebackend.preferences

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserPreferencesRepository : JpaRepository<UserPreferences, Long> {
    fun findByUserId(userId: Long): Optional<UserPreferences>
    fun existsByUserId(userId: Long): Boolean
}
