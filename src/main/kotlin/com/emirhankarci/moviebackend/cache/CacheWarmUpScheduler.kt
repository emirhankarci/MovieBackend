package com.emirhankarci.moviebackend.cache

import com.emirhankarci.moviebackend.featured.FeaturedMoviesResult
import com.emirhankarci.moviebackend.featured.FeaturedMoviesService
import com.emirhankarci.moviebackend.featured.TimeWindow
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.Duration

@Component
@ConditionalOnProperty(
    name = ["scheduler.cache-warmup.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class CacheWarmUpScheduler(
    private val featuredMoviesService: FeaturedMoviesService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CacheWarmUpScheduler::class.java)
        private const val DEFAULT_LIMIT = 10
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 5000L
    }

    /**
     * Warm up cache when application starts
     */
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        logger.info("Application ready - starting initial cache warm-up")
        warmUpCache()
    }

    /**
     * Periodically refresh cache (every 4 hours by default)
     */
    @Scheduled(fixedRateString = "\${scheduler.cache-warmup.interval:14400000}")
    fun scheduledWarmUp() {
        logger.info("Scheduled cache warm-up triggered")
        warmUpCache()
    }

    /**
     * Warm up featured movies cache for both daily and weekly time windows
     */
    private fun warmUpCache() {
        val startTime = LocalDateTime.now()
        logger.info("=== Cache Warm-Up Started at {} ===", startTime)

        var successCount = 0
        var errorCount = 0

        // Warm up daily trending
        if (warmUpTimeWindow(TimeWindow.DAY)) {
            successCount++
        } else {
            errorCount++
        }

        // Warm up weekly trending
        if (warmUpTimeWindow(TimeWindow.WEEK)) {
            successCount++
        } else {
            errorCount++
        }

        val endTime = LocalDateTime.now()
        val duration = Duration.between(startTime, endTime)

        logger.info("=== Cache Warm-Up Completed ===")
        logger.info("Duration: {} ms", duration.toMillis())
        logger.info("Success: {}/2, Errors: {}/2", successCount, errorCount)
    }

    /**
     * Warm up cache for a specific time window with retry logic
     */
    private fun warmUpTimeWindow(timeWindow: TimeWindow): Boolean {
        var attempt = 0
        
        while (attempt < MAX_RETRIES) {
            attempt++
            
            try {
                logger.info("Warming up {} trending cache (attempt {}/{})", 
                    timeWindow.value, attempt, MAX_RETRIES)
                
                val result = featuredMoviesService.getFeaturedMovies(timeWindow, DEFAULT_LIMIT)
                
                when (result) {
                    is FeaturedMoviesResult.Success -> {
                        logger.info("Successfully cached {} {} trending movies", 
                            result.movies.size, timeWindow.value)
                        return true
                    }
                    is FeaturedMoviesResult.Error -> {
                        logger.warn("Failed to warm up {} cache: {} - {}", 
                            timeWindow.value, result.code, result.message)
                        
                        if (attempt < MAX_RETRIES) {
                            logger.info("Retrying in {} ms...", RETRY_DELAY_MS)
                            Thread.sleep(RETRY_DELAY_MS)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Exception during {} cache warm-up (attempt {}): {}", 
                    timeWindow.value, attempt, e.message, e)
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return false
                    }
                }
            }
        }

        logger.error("Failed to warm up {} cache after {} attempts", timeWindow.value, MAX_RETRIES)
        return false
    }
}
