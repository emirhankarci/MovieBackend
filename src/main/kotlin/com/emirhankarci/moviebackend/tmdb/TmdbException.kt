package com.emirhankarci.moviebackend.tmdb

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * TMDB API Exception
 */
class TmdbApiException(
    override val message: String,
    val statusCode: Int
) : RuntimeException(message)

/**
 * Error Response DTO
 */
data class TmdbErrorResponse(
    val error: String,
    val message: String?,
    val statusCode: Int
)

/**
 * TMDB Exception Handler
 */
@ControllerAdvice
class TmdbExceptionHandler {

    @ExceptionHandler(TmdbApiException::class)
    fun handleTmdbApiException(ex: TmdbApiException): ResponseEntity<TmdbErrorResponse> {
        val response = when (ex.statusCode) {
            404 -> TmdbErrorResponse("Not Found", ex.message, 404)
            429 -> TmdbErrorResponse("Rate Limit Exceeded", ex.message, 429)
            401 -> TmdbErrorResponse("Unauthorized", ex.message, 401)
            else -> TmdbErrorResponse("Service Unavailable", ex.message, 503)
        }
        
        return ResponseEntity
            .status(HttpStatus.valueOf(response.statusCode))
            .body(response)
    }
}
