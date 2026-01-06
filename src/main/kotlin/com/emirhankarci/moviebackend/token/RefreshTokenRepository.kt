package com.emirhankarci.moviebackend.token

import com.emirhankarci.moviebackend.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun deleteByUser(user: User)
    
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.token = :token")
    fun revokeByToken(token: String): Int

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR r.revoked = true")
    fun deleteExpiredAndRevokedTokens(now: Instant): Int
    
    /**
     * Delete expired tokens (expiresAt < now)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    fun deleteExpiredTokens(now: Instant): Int
    
    /**
     * Delete revoked tokens older than specified time
     * This allows keeping recently revoked tokens for audit purposes
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true AND r.expiresAt < :cutoffTime")
    fun deleteRevokedTokensOlderThan(cutoffTime: Instant): Int
}
