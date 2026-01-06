package com.emirhankarci.moviebackend.exception

import org.springframework.http.HttpStatus

/**
 * Base Business Exception
 * Tüm business exception'lar bu class'tan türer.
 */
abstract class BusinessException(
    override val message: String,
    val errorCode: String,
    val httpStatus: HttpStatus
) : RuntimeException(message)

/**
 * Resource Not Found Exception (404)
 * Kaynak bulunamadığında fırlatılır.
 */
class ResourceNotFoundException(
    resource: String,
    id: Any
) : BusinessException(
    message = "$resource not found with id: $id",
    errorCode = "RESOURCE_NOT_FOUND",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * Validation Exception (400)
 * Input validation hatalarında fırlatılır.
 */
class ValidationException(
    message: String,
    val fieldErrors: List<FieldError> = emptyList()
) : BusinessException(
    message = message,
    errorCode = "VALIDATION_ERROR",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * Unauthorized Exception (401)
 * Authentication gerektiğinde fırlatılır.
 */
class UnauthorizedException(
    message: String = "Authentication required"
) : BusinessException(
    message = message,
    errorCode = "UNAUTHORIZED",
    httpStatus = HttpStatus.UNAUTHORIZED
)

/**
 * Forbidden Exception (403)
 * Yetki olmadığında fırlatılır.
 */
class ForbiddenException(
    message: String = "Access denied"
) : BusinessException(
    message = message,
    errorCode = "FORBIDDEN",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * External Service Exception (503)
 * Dış servisler çöktüğünde fırlatılır.
 */
abstract class ExternalServiceException(
    val serviceName: String,
    message: String,
    val originalStatusCode: Int? = null
) : BusinessException(
    message = message,
    errorCode = "EXTERNAL_SERVICE_ERROR",
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE
)

/**
 * TMDB Service Exception
 */
class TmdbServiceException(
    message: String,
    statusCode: Int? = null
) : ExternalServiceException("TMDB", message, statusCode)

/**
 * Gemini AI Service Exception
 */
class GeminiServiceException(
    message: String
) : ExternalServiceException("Gemini AI", message)

/**
 * Redis Cache Service Exception
 */
class RedisServiceException(
    message: String
) : ExternalServiceException("Redis Cache", message)
