package com.emirhankarci.moviebackend.exception

import com.emirhankarci.moviebackend.search.ExternalServiceException
import com.emirhankarci.moviebackend.search.InvalidGenreException
import com.emirhankarci.moviebackend.search.InvalidQueryException
import com.emirhankarci.moviebackend.search.InvalidRatingRangeException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(
    val status: Int,
    val message: String
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredToken(ex: ExpiredJwtException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(401, "Token expired!"))
    }

    @ExceptionHandler(JwtException::class)
    fun handleInvalidToken(ex: JwtException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(401, "Invalid token!"))
    }

    @ExceptionHandler(InvalidQueryException::class)
    fun handleInvalidQuery(ex: InvalidQueryException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(400, ex.message ?: "Invalid query"))
    }

    @ExceptionHandler(InvalidGenreException::class)
    fun handleInvalidGenre(ex: InvalidGenreException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(400, ex.message ?: "Invalid genre"))
    }

    @ExceptionHandler(InvalidRatingRangeException::class)
    fun handleInvalidRatingRange(ex: InvalidRatingRangeException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(400, ex.message ?: "Invalid rating range"))
    }

    @ExceptionHandler(ExternalServiceException::class)
    fun handleExternalService(ex: ExternalServiceException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse(503, ex.message ?: "External service unavailable"))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(500, ex.message ?: "Internal server error"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(500, "An unexpected error occurred"))
    }
}
