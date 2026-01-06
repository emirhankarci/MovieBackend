package com.emirhankarci.moviebackend.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByUsername(username: String): User?
    
    /**
     * Find all users who have preferences set (for daily suggestion generation)
     */
    @Query("""
        SELECT u FROM User u 
        WHERE EXISTS (SELECT 1 FROM UserPreferences p WHERE p.user.id = u.id)
    """)
    fun findAllUsersWithPreferences(pageable: Pageable): Page<User>
    
    /**
     * Count users with preferences
     */
    @Query("""
        SELECT COUNT(u) FROM User u 
        WHERE EXISTS (SELECT 1 FROM UserPreferences p WHERE p.user.id = u.id)
    """)
    fun countUsersWithPreferences(): Long
}