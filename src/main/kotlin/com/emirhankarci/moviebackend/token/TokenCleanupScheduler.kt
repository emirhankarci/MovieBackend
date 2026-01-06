package com.emirhankarci.moviebackend.token

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

@Component
@ConditionalOnProperty(
    name = ["scheduler.token-cleanup.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class TokenCleanupScheduler(
    private val refreshTokenRepository: RefreshTokenRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TokenCleanupScheduler::class.java)
    }

    @Value("\${scheduler.token-cleanup.revoked-retention-days:7}")
    private var revokedRetentionDays: Long = 7

    /**
     * Clean up expired and old revoked tokens
     * Runs every day at 03:00 AM
     */
    @Scheduled(cron = "\${scheduler.token-cleanup.cron:0 0 3 * * *}")
    @Transactional
    fun cleanupExpiredTokens() {
        val startTime = LocalDateTime.now()
        logger.info("=== Token Cleanup Started at {} ===", startTime)

        try {
            val now = Instant.now()
            
            // Delete expired tokens
            val expiredDeleted = refreshTokenRepository.deleteExpiredTokens(now)
            logger.info("Deleted {} expired tokens", expiredDeleted)
            
            // Delete revoked tokens older than retention period
            val cutoffTime = now.minus(Duration.ofDays(revokedRetentionDays))
            val revokedDeleted = refreshTokenRepository.deleteRevokedTokensOlderThan(cutoffTime)
            logger.info("Deleted {} revoked tokens older than {} days", revokedDeleted, revokedRetentionDays)
            
            val totalDeleted = expiredDeleted + revokedDeleted
            
            val endTime = LocalDateTime.now()
            val duration = Duration.between(startTime, endTime)
            
            logger.info("=== Token Cleanup Completed ===")
            logger.info("Duration: {} ms", duration.toMillis())
            logger.info("Total deleted: {} (expired: {}, revoked: {})", 
                totalDeleted, expiredDeleted, revokedDeleted)
                
        } catch (e: Exception) {
            logger.error("Token cleanup failed: {}", e.message, e)
            // Don't rethrow - allow scheduler to continue
        }
    }
}
