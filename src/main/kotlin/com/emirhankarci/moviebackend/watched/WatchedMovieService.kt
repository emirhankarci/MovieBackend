package com.emirhankarci.moviebackend.watched

import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WatchedMovieService(
    private val watchedMovieRepository: WatchedMovieRepository,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WatchedMovieService::class.java)
    }

    @Transactional
    fun addWatchedMovie(username: String, request: WatchedMovieRequest): WatchedMovieResult<String> {
        when (val validation = request.validate()) {
            is WatchedMovieValidationResult.Invalid -> {
                logger.warn("Validation failed for user {}: {}", username, validation.message)
                return WatchedMovieResult.Error(validation.message)
            }
            is WatchedMovieValidationResult.Valid -> { /* continue */ }
        }

        val user = userRepository.findByUsername(username)
            ?: return WatchedMovieResult.Error("User not found!")

        val existing = watchedMovieRepository.findByUserIdAndMovieId(user.id!!, request.movieId)

        return if (existing.isPresent) {
            logger.warn("Movie {} already in watched list for user {}", request.movieId, username)
            WatchedMovieResult.Error("Movie is already in your watched list")
        } else {
            val watchedMovie = WatchedMovie(
                user = user,
                movieId = request.movieId,
                movieTitle = request.movieTitle,
                posterPath = request.posterPath
            )
            watchedMovieRepository.save(watchedMovie)
            logger.info("Movie {} added to watched list for user {}", request.movieId, username)
            WatchedMovieResult.Success("Movie marked as watched")
        }
    }

    @Transactional
    fun removeWatchedMovie(username: String, movieId: Long): WatchedMovieResult<String> {
        val user = userRepository.findByUsername(username)
            ?: return WatchedMovieResult.Error("User not found!")

        val existing = watchedMovieRepository.findByUserIdAndMovieId(user.id!!, movieId)

        return if (existing.isPresent) {
            watchedMovieRepository.delete(existing.get())
            logger.info("Movie {} removed from watched list for user {}", movieId, username)
            WatchedMovieResult.Success("Movie removed from watched list")
        } else {
            logger.warn("Movie {} not found in watched list for user {}", movieId, username)
            WatchedMovieResult.Error("Movie not found in watched list")
        }
    }

    fun getUserWatchedMovies(
        username: String,
        sortOrder: String = "desc"
    ): WatchedMovieResult<List<WatchedMovieResponse>> {
        val user = userRepository.findByUsername(username)
            ?: return WatchedMovieResult.Error("User not found!")

        val watchedMovies = if (sortOrder.lowercase() == "asc") {
            watchedMovieRepository.findByUserIdOrderByWatchedAtAsc(user.id!!)
        } else {
            watchedMovieRepository.findByUserIdOrderByWatchedAtDesc(user.id!!)
        }

        val response = watchedMovies.map {
            WatchedMovieResponse(
                id = it.id!!,
                movieId = it.movieId,
                movieTitle = it.movieTitle,
                posterPath = it.posterPath,
                watchedAt = it.watchedAt,
                userRating = it.userRating
            )
        }

        logger.debug("Returning {} watched movies for user {}", response.size, username)
        return WatchedMovieResult.Success(response)
    }

    fun isMovieWatched(username: String, movieId: Long): WatchedMovieResult<WatchedMovieStatusResponse> {
        val user = userRepository.findByUsername(username)
            ?: return WatchedMovieResult.Error("User not found!")

        val exists = watchedMovieRepository.existsByUserIdAndMovieId(user.id!!, movieId)
        logger.debug("Watched status check for user {}, movie {}: {}", username, movieId, exists)

        return WatchedMovieResult.Success(WatchedMovieStatusResponse(movieId, exists))
    }
}
