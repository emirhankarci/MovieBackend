package com.emirhankarci.moviebackend.resilience

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CircuitBreakerService(
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CircuitBreakerService::class.java)
        const val TMDB_CIRCUIT_BREAKER = "tmdb"
        const val GEMINI_CIRCUIT_BREAKER = "gemini"
    }

    /**
     * TMDB API çağrılarını circuit breaker ile sarar.
     * Circuit açıksa fallback değerini döndürür.
     */
    fun <T> executeWithTmdbCircuitBreaker(
        fallback: () -> T,
        supplier: () -> T
    ): T {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker(TMDB_CIRCUIT_BREAKER)
        return try {
            circuitBreaker.executeSupplier(supplier)
        } catch (e: CallNotPermittedException) {
            logger.warn("TMDB circuit breaker is OPEN, returning fallback response")
            fallback()
        } catch (e: Exception) {
            logger.error("TMDB API call failed: {}", e.message)
            throw e
        }
    }

    /**
     * Gemini API çağrılarını circuit breaker ile sarar.
     * Circuit açıksa fallback değerini döndürür.
     */
    fun <T> executeWithGeminiCircuitBreaker(
        fallback: () -> T,
        supplier: () -> T
    ): T {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker(GEMINI_CIRCUIT_BREAKER)
        return try {
            circuitBreaker.executeSupplier(supplier)
        } catch (e: CallNotPermittedException) {
            logger.warn("Gemini circuit breaker is OPEN, returning fallback response")
            fallback()
        } catch (e: Exception) {
            logger.error("Gemini API call failed: {}", e.message)
            throw e
        }
    }

    /**
     * TMDB circuit breaker durumunu döndürür.
     */
    fun getTmdbCircuitBreakerState(): String {
        return circuitBreakerRegistry.circuitBreaker(TMDB_CIRCUIT_BREAKER).state.name
    }

    /**
     * Gemini circuit breaker durumunu döndürür.
     */
    fun getGeminiCircuitBreakerState(): String {
        return circuitBreakerRegistry.circuitBreaker(GEMINI_CIRCUIT_BREAKER).state.name
    }

    /**
     * Tüm circuit breaker durumlarını döndürür.
     */
    fun getAllCircuitBreakerStates(): Map<String, String> {
        return mapOf(
            TMDB_CIRCUIT_BREAKER to getTmdbCircuitBreakerState(),
            GEMINI_CIRCUIT_BREAKER to getGeminiCircuitBreakerState()
        )
    }
}
