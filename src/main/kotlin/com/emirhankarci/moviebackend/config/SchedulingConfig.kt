package com.emirhankarci.moviebackend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@EnableScheduling
@EnableAsync
class SchedulingConfig {

    @Bean
    fun taskScheduler(): ThreadPoolTaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 3
        scheduler.setThreadNamePrefix("scheduled-task-")
        scheduler.setErrorHandler { throwable ->
            // Log error but don't propagate - allows other tasks to continue
            org.slf4j.LoggerFactory.getLogger(SchedulingConfig::class.java)
                .error("Scheduled task error: {}", throwable.message, throwable)
        }
        scheduler.initialize()
        return scheduler
    }
}
