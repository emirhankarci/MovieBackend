package com.emirhankarci.moviebackend.usercollection

import com.emirhankarci.moviebackend.common.PageResponse
import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserCollectionService(
    private val userCollectionRepository: UserCollectionRepository,
    private val userCollectionMovieRepository: UserCollectionMovieRepository,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UserCollectionService::class.java)
        private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
    }

    /**
     * Format poster path as full URL
     */
    private fun formatPosterUrl(posterPath: String?): String? {
        return posterPath?.let { "$TMDB_IMAGE_BASE_URL$it" }
    }

    /**
     * Get cover posters for a collection (first 4 movies' posters)
     * Returns a list of 4 elements, with null for missing posters
     */
    private fun getCoverPosters(collectionId: Long): List<String?> {
        val first4Movies = userCollectionMovieRepository.findTop4ByCollectionIdOrderByAddedAtAsc(collectionId)
        val posters = first4Movies.map { formatPosterUrl(it.posterPath) }
        // Pad with nulls to always return 4 elements
        return posters + List(4 - posters.size) { null }
    }

    @Transactional
    fun createCollection(username: String, request: CreateCollectionRequest): UserCollectionResult<CollectionResponse> {
        val user = userRepository.findByUsername(username)
            ?: return UserCollectionResult.Error("USER_NOT_FOUND", "User not found")

        if (userCollectionRepository.existsByUserIdAndName(user.id!!, request.name)) {
            logger.warn("Collection name '{}' already exists for user {}", request.name, username)
            return UserCollectionResult.Error("DUPLICATE_NAME", "A collection with this name already exists")
        }

        val collection = UserCollection(
            user = user,
            name = request.name,
            description = request.description
        )

        val saved = userCollectionRepository.save(collection)
        logger.info("Collection '{}' created for user {}", request.name, username)

        return UserCollectionResult.Success(
            CollectionResponse(
                id = saved.id!!,
                name = saved.name,
                description = saved.description,
                createdAt = saved.createdAt,
                updatedAt = saved.updatedAt
            )
        )
    }


    fun getUserCollections(
        username: String,
        page: Int = 0,
        size: Int = 20
    ): UserCollectionResult<PageResponse<CollectionSummaryResponse>> {
        val user = userRepository.findByUsername(username)
            ?: return UserCollectionResult.Error("USER_NOT_FOUND", "User not found")

        val pageable = PageRequest.of(page, size)
        val collectionsPage = userCollectionRepository.findByUserIdOrderByCreatedAtDesc(user.id!!, pageable)

        val response = PageResponse.from(collectionsPage) { collection ->
            val collectionId = collection.id!!
            val movieCount = userCollectionMovieRepository.countByCollectionId(collectionId)
            val coverPosters = getCoverPosters(collectionId)

            CollectionSummaryResponse(
                id = collectionId,
                name = collection.name,
                description = collection.description,
                coverPosters = coverPosters,
                movieCount = movieCount,
                createdAt = collection.createdAt
            )
        }

        logger.debug("Returning {} collections for user {} (page {})", response.content.size, username, page)
        return UserCollectionResult.Success(response)
    }

    fun getCollectionDetail(
        username: String,
        collectionId: Long,
        page: Int = 0,
        size: Int = 20,
        sortOrder: String = "desc"
    ): UserCollectionResult<CollectionDetailResponse> {
        val user = userRepository.findByUsername(username)
            ?: return UserCollectionResult.Error("USER_NOT_FOUND", "User not found")

        val collection = userCollectionRepository.findByIdAndUserId(collectionId, user.id!!)
            .orElse(null) ?: return UserCollectionResult.Error("COLLECTION_NOT_FOUND", "Collection not found")

        val pageable = PageRequest.of(page, size)
        val moviesPage = if (sortOrder.lowercase() == "asc") {
            userCollectionMovieRepository.findByCollectionIdOrderByAddedAtAsc(collectionId, pageable)
        } else {
            userCollectionMovieRepository.findByCollectionIdOrderByAddedAtDesc(collectionId, pageable)
        }

        val coverPosters = getCoverPosters(collectionId)
        val totalMovieCount = userCollectionMovieRepository.countByCollectionId(collectionId)

        val moviesResponse = PageResponse.from(moviesPage) { movie ->
            CollectionMovieResponse(
                id = movie.id!!,
                movieId = movie.movieId,
                movieTitle = movie.movieTitle,
                posterPath = formatPosterUrl(movie.posterPath),
                addedAt = movie.addedAt
            )
        }

        return UserCollectionResult.Success(
            CollectionDetailResponse(
                id = collection.id!!,
                name = collection.name,
                description = collection.description,
                coverPosters = coverPosters,
                movieCount = totalMovieCount,
                createdAt = collection.createdAt,
                movies = moviesResponse
            )
        )
    }

    @Transactional
    fun updateCollection(
        username: String,
        collectionId: Long,
        request: UpdateCollectionRequest
    ): UserCollectionResult<CollectionResponse> {
        val user = userRepository.findByUsername(username)
            ?: return UserCollectionResult.Error("USER_NOT_FOUND", "User not found")

        val userId = user.id!!
        val collection = userCollectionRepository.findByIdAndUserId(collectionId, userId)
            .orElse(null) ?: return UserCollectionResult.Error("COLLECTION_NOT_FOUND", "Collection not found")

        // Check for duplicate name if name is being updated
        if (request.name != null && request.name != collection.name) {
            if (userCollectionRepository.existsByUserIdAndNameAndIdNot(userId, request.name, collectionId)) {
                logger.warn("Collection name '{}' already exists for user {}", request.name, username)
                return UserCollectionResult.Error("DUPLICATE_NAME", "A collection with this name already exists")
            }
        }

        val updated = collection.copy(
            name = request.name ?: collection.name,
            description = request.description ?: collection.description,
            updatedAt = LocalDateTime.now()
        )

        val saved = userCollectionRepository.save(updated)
        logger.info("Collection {} updated for user {}", collectionId, username)

        return UserCollectionResult.Success(
            CollectionResponse(
                id = saved.id!!,
                name = saved.name,
                description = saved.description,
                createdAt = saved.createdAt,
                updatedAt = saved.updatedAt
            )
        )
    }

    @Transactional
    fun deleteCollection(username: String, collectionId: Long): UserCollectionResult<String> {
        val user = userRepository.findByUsername(username)
            ?: return UserCollectionResult.Error("USER_NOT_FOUND", "User not found")

        val collection = userCollectionRepository.findByIdAndUserId(collectionId, user.id!!)
            .orElse(null) ?: return UserCollectionResult.Error("COLLECTION_NOT_FOUND", "Collection not found")

        userCollectionRepository.delete(collection)
        logger.info("Collection {} deleted for user {}", collectionId, username)

        return UserCollectionResult.Success("Collection deleted successfully")
    }

    @Transactional
    fun addMovieToCollection(
        username: String,
        collectionId: Long,
        request: AddMovieRequest
    ): UserCollectionResult<String> {
        val user = userRepository.findByUsername(username)
            ?: return UserCollectionResult.Error("USER_NOT_FOUND", "User not found")

        val collection = userCollectionRepository.findByIdAndUserId(collectionId, user.id!!)
            .orElse(null) ?: return UserCollectionResult.Error("COLLECTION_NOT_FOUND", "Collection not found")

        if (userCollectionMovieRepository.existsByCollectionIdAndMovieId(collectionId, request.movieId)) {
            logger.warn("Movie {} already exists in collection {} for user {}", request.movieId, collectionId, username)
            return UserCollectionResult.Error("DUPLICATE_MOVIE", "Movie already exists in this collection")
        }

        val collectionMovie = UserCollectionMovie(
            collection = collection,
            movieId = request.movieId,
            movieTitle = request.movieTitle,
            posterPath = request.posterPath
        )

        userCollectionMovieRepository.save(collectionMovie)
        logger.info("Movie {} added to collection {} for user {}", request.movieId, collectionId, username)

        return UserCollectionResult.Success("Movie added to collection")
    }

    @Transactional
    fun removeMovieFromCollection(
        username: String,
        collectionId: Long,
        movieId: Long
    ): UserCollectionResult<String> {
        val user = userRepository.findByUsername(username)
            ?: return UserCollectionResult.Error("USER_NOT_FOUND", "User not found")

        val collection = userCollectionRepository.findByIdAndUserId(collectionId, user.id!!)
            .orElse(null) ?: return UserCollectionResult.Error("COLLECTION_NOT_FOUND", "Collection not found")

        val collectionMovie = userCollectionMovieRepository.findByCollectionIdAndMovieId(collectionId, movieId)
            .orElse(null) ?: return UserCollectionResult.Error("MOVIE_NOT_IN_COLLECTION", "Movie not found in collection")

        userCollectionMovieRepository.delete(collectionMovie)
        logger.info("Movie {} removed from collection {} for user {}", movieId, collectionId, username)

        return UserCollectionResult.Success("Movie removed from collection")
    }

    fun getMovieCollectionStatus(username: String, movieId: Long): UserCollectionResult<MovieCollectionStatusResponse> {
        val user = userRepository.findByUsername(username)
            ?: return UserCollectionResult.Error("USER_NOT_FOUND", "User not found")

        val collectionMovies = userCollectionMovieRepository.findByMovieIdAndCollectionUserId(movieId, user.id!!)

        val collections = collectionMovies.map { movie ->
            val coll = movie.collection
            CollectionInfo(
                id = coll.id!!,
                name = coll.name
            )
        }

        return UserCollectionResult.Success(
            MovieCollectionStatusResponse(
                movieId = movieId,
                collections = collections
            )
        )
    }
}
