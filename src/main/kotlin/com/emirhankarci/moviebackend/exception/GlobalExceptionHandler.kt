package com.emirhankarci.moviebackend.exception

import com.emirhankarci.moviebackend.search.ExternalServiceException
import com.emirhankarci.moviebackend.search.InvalidGenreException
import com.emirhankarci.moviebackend.search.InvalidQueryException
import com.emirhankarci.moviebackend.search.InvalidRatingRangeException
import com.emirhankarci.moviebackend.tmdb.TmdbApiException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.*

/**
 * Global Exception Handler
 * Tüm exception'ları merkezi olarak handle eder.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ==================== Business Exceptions ====================

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logException(ex, request, ex.httpStatus)
        
        return buildResponse(
            status = ex.httpStatus,
            error = ex.errorCode,
            message = ex.message,
            path = request.requestURI,
            details = if (ex is ValidationException) ex.fieldErrors else null
        )
    }

    // ==================== Validation Exceptions ====================

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map { 
            FieldError(it.field, it.defaultMessage ?: "Invalid value")
        }
        logWarn("Validation failed: ${fieldErrors.size} errors", request)
        
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "VALIDATION_ERROR",
            message = "Validation failed",
            path = request.requestURI,
            details = fieldErrors
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val fieldErrors = ex.constraintViolations.map {
            FieldError(it.propertyPath.toString(), it.message)
        }
        logWarn("Constraint violation: ${fieldErrors.size} errors", request)
        
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "VALIDATION_ERROR",
            message = "Constraint violation",
            path = request.requestURI,
            details = fieldErrors
        )
    }

    // ==================== JWT Exceptions ====================

    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredToken(
        ex: ExpiredJwtException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logWarn("Token expired", request)
        return buildResponse(
            status = HttpStatus.UNAUTHORIZED,
            error = "TOKEN_EXPIRED",
            message = "Token expired",
            path = request.requestURI
        )
    }

    @ExceptionHandler(JwtException::class)
    fun handleInvalidToken(
        ex: JwtException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logWarn("Invalid token: ${ex.message}", request)
        return buildResponse(
            status = HttpStatus.UNAUTHORIZED,
            error = "INVALID_TOKEN",
            message = "Invalid token",
            path = request.requestURI
        )
    }

    // ==================== Security Exceptions ====================

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logWarn("Access denied", request)
        return buildResponse(
            status = HttpStatus.FORBIDDEN,
            error = "ACCESS_DENIED",
            message = "Access denied",
            path = request.requestURI
        )
    }

    // ==================== TMDB Exception ====================

    @ExceptionHandler(TmdbApiException::class)
    fun handleTmdbApiException(
        ex: TmdbApiException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val status = when (ex.statusCode) {
            404 -> HttpStatus.NOT_FOUND
            429 -> HttpStatus.TOO_MANY_REQUESTS
            401 -> HttpStatus.UNAUTHORIZED
            else -> HttpStatus.SERVICE_UNAVAILABLE
        }
        logException(ex, request, status)
        
        return buildResponse(
            status = status,
            error = "TMDB_ERROR",
            message = ex.message,
            path = request.requestURI
        )
    }

    // ==================== Legacy Search Exceptions ====================

    @ExceptionHandler(InvalidQueryException::class)
    fun handleInvalidQuery(
        ex: InvalidQueryException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logWarn("Invalid query: ${ex.message}", request)
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "INVALID_QUERY",
            message = ex.message ?: "Invalid query",
            path = request.requestURI
        )
    }

    @ExceptionHandler(InvalidGenreException::class)
    fun handleInvalidGenre(
        ex: InvalidGenreException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logWarn("Invalid genre: ${ex.message}", request)
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "INVALID_GENRE",
            message = ex.message ?: "Invalid genre",
            path = request.requestURI
        )
    }

    @ExceptionHandler(InvalidRatingRangeException::class)
    fun handleInvalidRatingRange(
        ex: InvalidRatingRangeException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logWarn("Invalid rating range: ${ex.message}", request)
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "INVALID_RATING_RANGE",
            message = ex.message ?: "Invalid rating range",
            path = request.requestURI
        )
    }

    @ExceptionHandler(ExternalServiceException::class)
    fun handleExternalService(
        ex: ExternalServiceException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logError("External service error", ex, request)
        return buildResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            error = "EXTERNAL_SERVICE_ERROR",
            message = ex.message ?: "External service unavailable",
            path = request.requestURI
        )
    }

    // ==================== Generic Exceptions ====================

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        ex: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logError("Illegal state", ex, request)
        return buildResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            error = "ILLEGAL_STATE",
            message = ex.message ?: "Internal server error",
            path = request.requestURI
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logError("Unexpected error", ex, request)
        return buildResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            error = "INTERNAL_ERROR",
            message = "An unexpected error occurred",
            path = request.requestURI
        )
    }

    // ==================== Helper Methods ====================

    private fun buildResponse(
        status: HttpStatus,
        error: String,
        message: String,
        path: String,
        details: List<FieldError>? = null
    ): ResponseEntity<ApiErrorResponse> {
        val response = ApiErrorResponse(
            status = status.value(),
            error = error,
            message = message,
            timestamp = Instant.now().toString(),
            path = path,
            traceId = generateTraceId(),
            details = details
        )
        return ResponseEntity.status(status).body(response)
    }

    private fun generateTraceId(): String {
        return UUID.randomUUID().toString().take(8)
    }

    private fun logWarn(message: String, request: HttpServletRequest) {
        logger.warn("[{}] {} - Path: {}", generateTraceId(), message, request.requestURI)
    }

    private fun logError(message: String, ex: Exception, request: HttpServletRequest) {
        logger.error("[{}] {} - Path: {} - Error: {}", generateTraceId(), message, request.requestURI, ex.message, ex)
    }

    private fun logException(ex: Exception, request: HttpServletRequest, status: HttpStatus) {
        if (status.is5xxServerError) {
            logger.error("Exception [{}] - Path: {} - Status: {} - {}", 
                generateTraceId(), request.requestURI, status.value(), ex.message, ex)
        } else {
            logger.warn("Exception [{}] - Path: {} - Status: {} - {}", 
                generateTraceId(), request.requestURI, status.value(), ex.message)
        }
    }
}
