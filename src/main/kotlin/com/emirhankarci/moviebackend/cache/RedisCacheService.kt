package com.emirhankarci.moviebackend.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisCacheService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) : CacheService {

    companion object {
        private val logger = LoggerFactory.getLogger(RedisCacheService::class.java)
    }

    @Value("\${cache.default-ttl-hours:24}")
    private var defaultTtlHours: Long = 24

    override fun <T> get(key: String, type: Class<T>): T? {
        return try {
            val value = redisTemplate.opsForValue().get(key)
            if (value != null) {
                logger.debug("Cache HIT for key: {}", key)
                // String olarak saklandıysa JSON'dan deserialize et
                when (value) {
                    is String -> objectMapper.readValue(value, type)
                    else -> objectMapper.convertValue(value, type)
                }
            } else {
                logger.debug("Cache MISS for key: {}", key)
                null
            }
        } catch (e: Exception) {
            logger.error("Cache GET failed for key {}: {}", key, e.message)
            null
        }
    }

    override fun <T> set(key: String, value: T, ttl: Duration) {
        try {
            // JSON string olarak sakla
            val jsonValue = objectMapper.writeValueAsString(value)
            redisTemplate.opsForValue().set(key, jsonValue, ttl)
            logger.debug("Cache SET for key: {} with TTL: {}", key, ttl)
        } catch (e: Exception) {
            logger.error("Cache SET failed for key {}: {}", key, e.message)
        }
    }

    override fun <T> set(key: String, value: T) {
        set(key, value, Duration.ofHours(defaultTtlHours))
    }

    override fun delete(key: String): Boolean {
        return try {
            val result = redisTemplate.delete(key)
            logger.debug("Cache DELETE for key: {}, result: {}", key, result)
            result
        } catch (e: Exception) {
            logger.error("Cache DELETE failed for key {}: {}", key, e.message)
            false
        }
    }

    override fun exists(key: String): Boolean {
        return try {
            val result = redisTemplate.hasKey(key)
            logger.debug("Cache EXISTS for key: {}, result: {}", key, result)
            result
        } catch (e: Exception) {
            logger.error("Cache EXISTS check failed for key {}: {}", key, e.message)
            false
        }
    }

    override fun deleteByPattern(pattern: String): Long {
        return try {
            val keys = redisTemplate.keys(pattern)
            if (keys.isNotEmpty()) {
                val count = redisTemplate.delete(keys)
                logger.info("Cache DELETE by pattern '{}': {} keys deleted", pattern, count)
                count
            } else {
                logger.debug("Cache DELETE by pattern '{}': no keys found", pattern)
                0L
            }
        } catch (e: Exception) {
            logger.error("Cache DELETE by pattern failed for '{}': {}", pattern, e.message)
            0L
        }
    }
}
