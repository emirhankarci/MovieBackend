package com.emirhankarci.moviebackend.token

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class TokenCleanupScheduler(
    private val refreshTokenRepository: RefreshTokenRepository
) {

    // Her gün gece 3'te çalışır
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun cleanupExpiredTokens() {
        val deleted = refreshTokenRepository.deleteExpiredAndRevokedTokens(Instant.now())
        println("Cleaned up $deleted expired/revoked refresh tokens")
    }
}
