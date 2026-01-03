package com.emirhankarci.moviebackend.usercollection

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user-collections")
class UserCollectionController(
    private val userCollectionService: UserCollectionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UserCollectionController::class.java)
    }

    @PostMapping
    fun createCollection(@RequestBody request: CreateCollectionRequest): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        logger.info("Creating collection '{}' for user {}", request.name, username)

        return when (val result = userCollectionService.createCollection(username, request)) {
            is UserCollectionResult.Success -> ResponseEntity.status(201).body(result.data)
            is UserCollectionResult.Error -> {
                val status = when (result.code) {
                    "VALIDATION_ERROR" -> 400
                    "DUPLICATE_NAME" -> 409
                    "USER_NOT_FOUND" -> 404
                    else -> 500
                }
                ResponseEntity.status(status).body(
                    UserCollectionErrorResponse(error = result.code, message = result.message)
                )
            }
        }
    }

    @GetMapping
    fun getUserCollections(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = userCollectionService.getUserCollections(username)) {
            is UserCollectionResult.Success -> ResponseEntity.ok(result.data)
            is UserCollectionResult.Error -> {
                ResponseEntity.status(404).body(
                    UserCollectionErrorResponse(error = result.code, message = result.message)
                )
            }
        }
    }

    @GetMapping("/{id}")
    fun getCollectionDetail(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "desc") sortOrder: String
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = userCollectionService.getCollectionDetail(username, id, sortOrder)) {
            is UserCollectionResult.Success -> ResponseEntity.ok(result.data)
            is UserCollectionResult.Error -> {
                val status = when (result.code) {
                    "COLLECTION_NOT_FOUND" -> 404
                    "USER_NOT_FOUND" -> 404
                    else -> 500
                }
                ResponseEntity.status(status).body(
                    UserCollectionErrorResponse(error = result.code, message = result.message)
                )
            }
        }
    }

    @PutMapping("/{id}")
    fun updateCollection(
        @PathVariable id: Long,
        @RequestBody request: UpdateCollectionRequest
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        logger.info("Updating collection {} for user {}", id, username)

        return when (val result = userCollectionService.updateCollection(username, id, request)) {
            is UserCollectionResult.Success -> ResponseEntity.ok(result.data)
            is UserCollectionResult.Error -> {
                val status = when (result.code) {
                    "VALIDATION_ERROR" -> 400
                    "DUPLICATE_NAME" -> 409
                    "COLLECTION_NOT_FOUND" -> 404
                    "USER_NOT_FOUND" -> 404
                    else -> 500
                }
                ResponseEntity.status(status).body(
                    UserCollectionErrorResponse(error = result.code, message = result.message)
                )
            }
        }
    }

    @DeleteMapping("/{id}")
    fun deleteCollection(@PathVariable id: Long): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        logger.info("Deleting collection {} for user {}", id, username)

        return when (val result = userCollectionService.deleteCollection(username, id)) {
            is UserCollectionResult.Success -> ResponseEntity.ok(mapOf("message" to result.data))
            is UserCollectionResult.Error -> {
                val status = when (result.code) {
                    "COLLECTION_NOT_FOUND" -> 404
                    "USER_NOT_FOUND" -> 404
                    else -> 500
                }
                ResponseEntity.status(status).body(
                    UserCollectionErrorResponse(error = result.code, message = result.message)
                )
            }
        }
    }

    @PostMapping("/{id}/movies")
    fun addMovieToCollection(
        @PathVariable id: Long,
        @RequestBody request: AddMovieRequest
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        logger.info("Adding movie {} to collection {} for user {}", request.movieId, id, username)

        return when (val result = userCollectionService.addMovieToCollection(username, id, request)) {
            is UserCollectionResult.Success -> ResponseEntity.status(201).body(mapOf("message" to result.data))
            is UserCollectionResult.Error -> {
                val status = when (result.code) {
                    "VALIDATION_ERROR" -> 400
                    "DUPLICATE_MOVIE" -> 409
                    "COLLECTION_NOT_FOUND" -> 404
                    "USER_NOT_FOUND" -> 404
                    else -> 500
                }
                ResponseEntity.status(status).body(
                    UserCollectionErrorResponse(error = result.code, message = result.message)
                )
            }
        }
    }

    @DeleteMapping("/{id}/movies/{movieId}")
    fun removeMovieFromCollection(
        @PathVariable id: Long,
        @PathVariable movieId: Long
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        logger.info("Removing movie {} from collection {} for user {}", movieId, id, username)

        return when (val result = userCollectionService.removeMovieFromCollection(username, id, movieId)) {
            is UserCollectionResult.Success -> ResponseEntity.ok(mapOf("message" to result.data))
            is UserCollectionResult.Error -> {
                val status = when (result.code) {
                    "MOVIE_NOT_IN_COLLECTION" -> 404
                    "COLLECTION_NOT_FOUND" -> 404
                    "USER_NOT_FOUND" -> 404
                    else -> 500
                }
                ResponseEntity.status(status).body(
                    UserCollectionErrorResponse(error = result.code, message = result.message)
                )
            }
        }
    }

    @GetMapping("/movie-status/{movieId}")
    fun getMovieCollectionStatus(@PathVariable movieId: Long): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = userCollectionService.getMovieCollectionStatus(username, movieId)) {
            is UserCollectionResult.Success -> ResponseEntity.ok(result.data)
            is UserCollectionResult.Error -> {
                ResponseEntity.status(404).body(
                    UserCollectionErrorResponse(error = result.code, message = result.message)
                )
            }
        }
    }
}
