package com.emirhankarci.moviebackend.chat

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping("/send")
    fun sendMessage(@RequestBody request: SendMessageRequest): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ChatErrorResponse("Authentication required", "UNAUTHORIZED"))

        // Validate request
        when (val validation = request.validate()) {
            is ChatValidationResult.Invalid -> {
                return ResponseEntity.badRequest()
                    .body(ChatErrorResponse(validation.message, "VALIDATION_ERROR"))
            }
            is ChatValidationResult.Valid -> { /* continue */ }
        }

        return when (val result = chatService.sendMessage(username, request.message)) {
            is ChatResult.Success -> ResponseEntity.ok(result.data)
            is ChatResult.Error -> {
                val status = when (result.code) {
                    ChatErrorCode.LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS
                    ChatErrorCode.USER_NOT_FOUND -> HttpStatus.NOT_FOUND
                    ChatErrorCode.AI_ERROR -> HttpStatus.SERVICE_UNAVAILABLE
                    ChatErrorCode.VALIDATION_ERROR -> HttpStatus.BAD_REQUEST
                    ChatErrorCode.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
                }
                ResponseEntity.status(status)
                    .body(ChatErrorResponse(result.message, result.code.name))
            }
        }
    }

    @GetMapping("/history")
    fun getHistory(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ChatErrorResponse("Authentication required", "UNAUTHORIZED"))

        return when (val result = chatService.getConversationHistory(username)) {
            is ChatResult.Success -> ResponseEntity.ok(result.data)
            is ChatResult.Error -> {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ChatErrorResponse(result.message, result.code.name))
            }
        }
    }

    @GetMapping("/quota")
    fun getQuota(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ChatErrorResponse("Authentication required", "UNAUTHORIZED"))

        return when (val result = chatService.getRemainingQuota(username)) {
            is ChatResult.Success -> ResponseEntity.ok(result.data)
            is ChatResult.Error -> {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ChatErrorResponse(result.message, result.code.name))
            }
        }
    }
}
