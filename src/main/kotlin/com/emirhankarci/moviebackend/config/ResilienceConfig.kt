package com.emirhankarci.moviebackend.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class ResilienceConfig {

    companion object {
        private val logger = LoggerFactory.getLogger(ResilienceConfig::class.java)
    }

    // TMDB Circuit Breaker Config
    @Value("\${resilience4j.circuitbreaker.tmdb.failure-rate-threshold:50}")
    private var tmdbFailureRateThreshold: Float = 50f

    @Value("\${resilience4j.circuitbreaker.tmdb.minimum-number-of-calls:5}")
    private var tmdbMinimumCalls: Int = 5

    @Value("\${resilience4j.circuitbreaker.tmdb.wait-duration-in-open-state:30}")
    private var tmdbWaitDuration: Long = 30

    @Value("\${resilience4j.circuitbreaker.tmdb.permitted-calls-in-half-open:3}")
    private var tmdbPermittedCallsInHalfOpen: Int = 3

    @Value("\${resilience4j.circuitbreaker.tmdb.sliding-window-size:10}")
    private var tmdbSlidingWindowSize: Int = 10

    // Gemini Circuit Breaker Config
    @Value("\${resilience4j.circuitbreaker.gemini.failure-rate-threshold:50}")
    private var geminiFailureRateThreshold: Float = 50f

    @Value("\${resilience4j.circuitbreaker.gemini.minimum-number-of-calls:3}")
    private var geminiMinimumCalls: Int = 3

    @Value("\${resilience4j.circuitbreaker.gemini.wait-duration-in-open-state:60}")
    private var geminiWaitDuration: Long = 60

    @Value("\${resilience4j.circuitbreaker.gemini.permitted-calls-in-half-open:2}")
    private var geminiPermittedCallsInHalfOpen: Int = 2

    @Value("\${resilience4j.circuitbreaker.gemini.sliding-window-size:5}")
    private var geminiSlidingWindowSize: Int = 5

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry {
        val tmdbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(tmdbFailureRateThreshold)
            .minimumNumberOfCalls(tmdbMinimumCalls)
            .waitDurationInOpenState(Duration.ofSeconds(tmdbWaitDuration))
            .permittedNumberOfCallsInHalfOpenState(tmdbPermittedCallsInHalfOpen)
            .slidingWindowSize(tmdbSlidingWindowSize)
            .build()

        val geminiConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(geminiFailureRateThreshold)
            .minimumNumberOfCalls(geminiMinimumCalls)
            .waitDurationInOpenState(Duration.ofSeconds(geminiWaitDuration))
            .permittedNumberOfCallsInHalfOpenState(geminiPermittedCallsInHalfOpen)
            .slidingWindowSize(geminiSlidingWindowSize)
            .build()

        val registry = CircuitBreakerRegistry.of(
            mapOf(
                "tmdb" to tmdbConfig,
                "gemini" to geminiConfig
            )
        )

        // Register event listeners for state transitions
        registerEventListeners(registry.circuitBreaker("tmdb"))
        registerEventListeners(registry.circuitBreaker("gemini"))

        logger.info("Circuit breaker registry initialized with TMDB and Gemini configurations")
        return registry
    }

    private fun registerEventListeners(circuitBreaker: CircuitBreaker) {
        circuitBreaker.eventPublisher
            .onStateTransition { event ->
                logger.info(
                    "Circuit breaker '{}' state changed: {} -> {}",
                    event.circuitBreakerName,
                    event.stateTransition.fromState,
                    event.stateTransition.toState
                )
            }
            .onError { event ->
                logger.warn(
                    "Circuit breaker '{}' recorded error: {}",
                    event.circuitBreakerName,
                    event.throwable.message
                )
            }
            .onSuccess { event ->
                logger.debug(
                    "Circuit breaker '{}' recorded success, duration: {}ms",
                    event.circuitBreakerName,
                    event.elapsedDuration.toMillis()
                )
            }
            .onCallNotPermitted { event ->
                logger.warn(
                    "Circuit breaker '{}' rejected call - circuit is OPEN",
                    event.circuitBreakerName
                )
            }
    }
}
