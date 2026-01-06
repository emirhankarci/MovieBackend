package com.emirhankarci.moviebackend.chat

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService,
    private val suggestionService: SuggestionService,
    private val reactionService: ReactionService
) {

    @PostMapping("/send")
    fun sendMessage(@Valid @RequestBody request: SendMessageRequest): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ChatErrorResponse("Authentication required", "UNAUTHORIZED"))

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
    fun getHistory(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ChatErrorResponse("Authentication required", "UNAUTHORIZED"))

        return when (val result = chatService.getConversationHistory(username, page, size)) {
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

    @GetMapping("/suggestions")
    fun getSuggestions(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ChatErrorResponse("Authentication required", "UNAUTHORIZED"))

        return when (val result = suggestionService.getSuggestions(username)) {
            is SuggestionResult.Success -> ResponseEntity.ok(result.data)
            is SuggestionResult.Error -> {
                val status = when (result.code) {
                    SuggestionErrorCode.USER_NOT_FOUND -> HttpStatus.NOT_FOUND
                    SuggestionErrorCode.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
                }
                ResponseEntity.status(status)
                    .body(ChatErrorResponse(result.message, result.code.name))
            }
        }
    }

    @PostMapping("/messages/{messageId}/reaction")
    fun addReaction(
        @PathVariable messageId: Long,
        @Valid @RequestBody request: ReactionRequest
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ChatErrorResponse("Authentication required", "UNAUTHORIZED"))

        return when (val result = reactionService.addReaction(username, messageId, request)) {
            is ReactionResult.Success -> ResponseEntity.ok(result.data)
            is ReactionResult.Error -> {
                val status = when (result.code) {
                    ReactionErrorCode.USER_NOT_FOUND -> HttpStatus.NOT_FOUND
                    ReactionErrorCode.MESSAGE_NOT_FOUND -> HttpStatus.NOT_FOUND
                    ReactionErrorCode.INVALID_REACTION -> HttpStatus.BAD_REQUEST
                    ReactionErrorCode.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
                }
                ResponseEntity.status(status)
                    .body(ChatErrorResponse(result.message, result.code.name))
            }
        }
    }
}
