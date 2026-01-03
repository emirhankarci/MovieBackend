package com.emirhankarci.moviebackend.collection

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/collections")
class CollectionController(
    private val collectionService: CollectionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CollectionController::class.java)
    }

    /**
     * Get prerequisite movies for a given movie
     * Returns movies that should be watched before the specified movie
     */
    @GetMapping("/prerequisites/{movieId}")
    fun getPrerequisiteMovies(@PathVariable movieId: Long): ResponseEntity<Any> {
        logger.info("Getting prerequisites for movie ID: {}", movieId)

        return when (val result = collectionService.getPrerequisiteMovies(movieId)) {
            is CollectionResult.Success -> {
                logger.info("Found {} prerequisites for movie {}", 
                    result.response.prerequisites.size, movieId)
                ResponseEntity.ok(result.response)
            }
            is CollectionResult.NoCollection -> {
                logger.info("Movie {} does not belong to any collection", movieId)
                ResponseEntity.ok(
                    NoCollectionResponse(message = result.message)
                )
            }
            is CollectionResult.NoPrerequisites -> {
                logger.info("Movie {} is first in collection {}", movieId, result.collectionName)
                ResponseEntity.ok(
                    NoPrerequisitesResponse(
                        collectionId = result.collectionId,
                        collectionName = result.collectionName,
                        message = result.message
                    )
                )
            }
            is CollectionResult.Error -> {
                val status = when (result.code) {
                    "MOVIE_NOT_FOUND" -> 404
                    "COLLECTION_NOT_FOUND" -> 404
                    "EXTERNAL_SERVICE_ERROR" -> 503
                    else -> 500
                }
                logger.error("Error getting prerequisites for movie {}: {} - {}", 
                    movieId, result.code, result.message)
                ResponseEntity.status(status).body(
                    CollectionErrorResponse(
                        error = result.code,
                        message = result.message
                    )
                )
            }
        }
    }
}
