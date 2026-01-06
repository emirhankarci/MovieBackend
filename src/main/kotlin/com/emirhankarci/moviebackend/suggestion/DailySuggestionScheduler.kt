package com.emirhankarci.moviebackend.suggestion

import com.emirhankarci.moviebackend.user.User
import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.Duration

@Component
@ConditionalOnProperty(
    name = ["scheduler.daily-suggestion.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class DailySuggestionScheduler(
    private val dailySuggestionService: DailySuggestionService,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DailySuggestionScheduler::class.java)
    }

    @Value("\${scheduler.daily-suggestion.batch-size:50}")
    private var batchSize: Int = 50

    /**
     * Generate daily suggestions for all users with preferences
     * Runs every day at 06:00 AM
     */
    @Scheduled(cron = "\${scheduler.daily-suggestion.cron:0 0 6 * * *}")
    fun generateDailySuggestions() {
        val startTime = LocalDateTime.now()
        logger.info("=== Daily Suggestion Generation Started at {} ===", startTime)

        var processedCount = 0
        var successCount = 0
        var errorCount = 0
        var skippedCount = 0
        var page = 0

        try {
            val totalUsers = userRepository.countUsersWithPreferences()
            logger.info("Total users with preferences: {}", totalUsers)

            if (totalUsers == 0L) {
                logger.info("No users with preferences found. Skipping generation.")
                return
            }

            // Process users in batches
            do {
                val pageable = PageRequest.of(page, batchSize)
                val usersPage = userRepository.findAllUsersWithPreferences(pageable)
                
                logger.info("Processing batch {} ({} users)", page + 1, usersPage.content.size)

                for (user in usersPage.content) {
                    processedCount++
                    
                    try {
                        val result = generateSuggestionForUser(user)
                        when (result) {
                            GenerationResult.SUCCESS -> successCount++
                            GenerationResult.SKIPPED -> skippedCount++
                            GenerationResult.ERROR -> errorCount++
                        }
                    } catch (e: Exception) {
                        errorCount++
                        logger.error("Unexpected error generating suggestions for user {}: {}", 
                            user.username, e.message, e)
                    }

                    // Log progress every 100 users
                    if (processedCount % 100 == 0) {
                        logger.info("Progress: {}/{} users processed", processedCount, totalUsers)
                    }
                }

                page++
            } while (usersPage.hasNext())

        } catch (e: Exception) {
            logger.error("Critical error in daily suggestion generation: {}", e.message, e)
        }

        val endTime = LocalDateTime.now()
        val duration = Duration.between(startTime, endTime)
        
        logger.info("=== Daily Suggestion Generation Completed ===")
        logger.info("Duration: {} seconds", duration.seconds)
        logger.info("Total processed: {}", processedCount)
        logger.info("Success: {}, Skipped: {}, Errors: {}", successCount, skippedCount, errorCount)
    }

    private fun generateSuggestionForUser(user: User): GenerationResult {
        return try {
            val result = dailySuggestionService.getDailySuggestions(user.username)
            
            when (result) {
                is SuggestionResult.Success -> {
                    if (result.data.cached) {
                        logger.debug("User {} already has suggestions for today (cached)", user.username)
                        GenerationResult.SKIPPED
                    } else {
                        logger.debug("Generated new suggestions for user {}", user.username)
                        GenerationResult.SUCCESS
                    }
                }
                is SuggestionResult.Error -> {
                    logger.warn("Failed to generate suggestions for user {}: {} ({})", 
                        user.username, result.message, result.code)
                    GenerationResult.ERROR
                }
            }
        } catch (e: Exception) {
            logger.error("Exception generating suggestions for user {}: {}", user.username, e.message)
            GenerationResult.ERROR
        }
    }

    private enum class GenerationResult {
        SUCCESS, SKIPPED, ERROR
    }
}
