package com.emirhankarci.moviebackend.exception

/**
 * Unified API Error Response
 * Tüm hata response'ları bu formatta döner.
 */
data class ApiErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: String,
    val path: String,
    val traceId: String? = null,
    val details: List<FieldError>? = null
)

/**
 * Validation hataları için field-level error
 */
data class FieldError(
    val field: String,
    val message: String
)
