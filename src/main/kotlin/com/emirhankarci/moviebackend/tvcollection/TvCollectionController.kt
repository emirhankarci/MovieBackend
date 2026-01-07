package com.emirhankarci.moviebackend.tvcollection

import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tv/collections")
class TvCollectionController(
    private val tvCollectionService: TvCollectionService,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvCollectionController::class.java)
    }

    @PostMapping
    fun createCollection(@RequestBody request: CreateTvCollectionRequest): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("POST /api/tv/collections - name: {}", request.name)

        return when (val result = tvCollectionService.createCollection(user, request)) {
            is TvCollectionResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.data as Any)
            is TvCollectionResult.Error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @GetMapping
    fun getCollections(): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/collections")

        val collections = tvCollectionService.getCollections(user)
        return ResponseEntity.ok(collections as Any)
    }

    @GetMapping("/{id}")
    fun getCollectionDetail(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/collections/{} - page: {}, size: {}", id, page, size)

        return when (val result = tvCollectionService.getCollectionDetail(user, id, page, size)) {
            is TvCollectionResult.Success -> ResponseEntity.ok(result.data as Any)
            is TvCollectionResult.Error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @PutMapping("/{id}")
    fun updateCollection(
        @PathVariable id: Long,
        @RequestBody request: UpdateTvCollectionRequest
    ): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("PUT /api/tv/collections/{}", id)

        return when (val result = tvCollectionService.updateCollection(user, id, request)) {
            is TvCollectionResult.Success -> ResponseEntity.ok(result.data as Any)
            is TvCollectionResult.Error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteCollection(@PathVariable id: Long): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("DELETE /api/tv/collections/{}", id)

        return when (val result = tvCollectionService.deleteCollection(user, id)) {
            is TvCollectionResult.Success -> ResponseEntity.noContent().build()
            is TvCollectionResult.Error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to result.code, "message" to result.message) as Any)
        }
    }

    @PostMapping("/{id}/series")
    fun addSeriesToCollection(
        @PathVariable id: Long,
        @RequestBody request: AddSeriesToCollectionRequest
    ): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("POST /api/tv/collections/{}/series - seriesId: {}", id, request.seriesId)

        return when (val result = tvCollectionService.addSeriesToCollection(user, id, request)) {
            is TvCollectionResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.data as Any)
            is TvCollectionResult.Error -> {
                val status = when (result.code) {
                    "COLLECTION_NOT_FOUND" -> HttpStatus.NOT_FOUND
                    "SERIES_ALREADY_IN_COLLECTION" -> HttpStatus.CONFLICT
                    else -> HttpStatus.BAD_REQUEST
                }
                ResponseEntity.status(status).body(mapOf("error" to result.code, "message" to result.message) as Any)
            }
        }
    }

    @DeleteMapping("/{id}/series/{seriesId}")
    fun removeSeriesFromCollection(
        @PathVariable id: Long,
        @PathVariable seriesId: Long
    ): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("DELETE /api/tv/collections/{}/series/{}", id, seriesId)

        return when (val result = tvCollectionService.removeSeriesFromCollection(user, id, seriesId)) {
            is TvCollectionResult.Success -> ResponseEntity.noContent().build()
            is TvCollectionResult.Error -> {
                val status = when (result.code) {
                    "COLLECTION_NOT_FOUND", "SERIES_NOT_IN_COLLECTION" -> HttpStatus.NOT_FOUND
                    else -> HttpStatus.BAD_REQUEST
                }
                ResponseEntity.status(status).body(mapOf("error" to result.code, "message" to result.message) as Any)
            }
        }
    }

    @GetMapping("/series/{seriesId}")
    fun getSeriesCollections(@PathVariable seriesId: Long): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return unauthorized()
        logger.info("GET /api/tv/collections/series/{}", seriesId)

        val response = tvCollectionService.getSeriesCollections(user, seriesId)
        return ResponseEntity.ok(response as Any)
    }

    private fun getCurrentUser() = SecurityContextHolder.getContext().authentication?.name
        ?.let { userRepository.findByUsername(it) }

    private fun unauthorized(): ResponseEntity<Any> = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(mapOf("error" to "UNAUTHORIZED", "message" to "Yetkilendirme gerekli") as Any)
}
