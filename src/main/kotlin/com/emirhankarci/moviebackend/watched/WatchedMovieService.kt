package com.emirhankarci.moviebackend.watched

import com.emirhankarci.moviebackend.common.PageResponse
import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

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
                posterPath = request.posterPath,
                imdbRating = request.imdbRating?.let { BigDecimal.valueOf(it) }
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
        page: Int = 0,
        size: Int = 20,
        sortOrder: String = "desc"
    ): WatchedMovieResult<PageResponse<WatchedMovieResponse>> {
        val user = userRepository.findByUsername(username)
            ?: return WatchedMovieResult.Error("User not found!")

        val pageable = PageRequest.of(page, size)
        val watchedMoviesPage = if (sortOrder.lowercase() == "asc") {
            watchedMovieRepository.findByUserIdOrderByWatchedAtAsc(user.id!!, pageable)
        } else {
            watchedMovieRepository.findByUserIdOrderByWatchedAtDesc(user.id!!, pageable)
        }

        val response = PageResponse.from(watchedMoviesPage) {
            WatchedMovieResponse(
                id = it.id!!,
                movieId = it.movieId,
                movieTitle = it.movieTitle,
                posterPath = it.posterPath,
                watchedAt = it.watchedAt,
                userRating = it.userRating?.toDouble(),
                imdbRating = it.imdbRating?.toDouble()
            )
        }

        logger.debug("Returning {} watched movies for user {} (page {})", response.content.size, username, page)
        return WatchedMovieResult.Success(response)
    }

    fun isMovieWatched(username: String, movieId: Long): WatchedMovieResult<WatchedMovieStatusResponse> {
        val user = userRepository.findByUsername(username)
            ?: return WatchedMovieResult.Error("User not found!")

        val watchedMovie = watchedMovieRepository.findByUserIdAndMovieId(user.id!!, movieId)
        val isWatched = watchedMovie.isPresent
        val userRating = watchedMovie.orElse(null)?.userRating?.toDouble()
        
        logger.debug("Watched status check for user {}, movie {}: watched={}, rating={}", username, movieId, isWatched, userRating)

        return WatchedMovieResult.Success(WatchedMovieStatusResponse(movieId, isWatched, userRating))
    }

    @Transactional
    fun rateMovie(username: String, request: RateMovieRequest): WatchedMovieResult<RateMovieResponse> {
        when (val validation = request.validate()) {
            is RatingValidationResult.Invalid -> {
                logger.warn("Rating validation failed for user {}: {}", username, validation.message)
                return WatchedMovieResult.Error(validation.message)
            }
            is RatingValidationResult.Valid -> { /* continue */ }
        }

        val user = userRepository.findByUsername(username)
            ?: return WatchedMovieResult.Error("User not found!")

        val existing = watchedMovieRepository.findByUserIdAndMovieId(user.id!!, request.movieId)
        val ratingAsBigDecimal = BigDecimal.valueOf(request.rating)

        return if (existing.isPresent) {
            // Movie already watched, update the rating and imdbRating if provided
            val existingMovie = existing.get()
            val updated = existingMovie.copy(
                userRating = ratingAsBigDecimal,
                imdbRating = request.imdbRating?.let { BigDecimal.valueOf(it) } ?: existingMovie.imdbRating
            )
            watchedMovieRepository.save(updated)
            logger.info("Rating updated to {} for movie {} by user {}", request.rating, request.movieId, username)
            WatchedMovieResult.Success(RateMovieResponse(
                movieId = request.movieId,
                rating = request.rating,
                isNewlyWatched = false,
                message = "Rating updated successfully"
            ))
        } else {
            // Movie not watched, add to watched list with rating
            val watchedMovie = WatchedMovie(
                user = user,
                movieId = request.movieId,
                movieTitle = request.movieTitle,
                posterPath = request.posterPath,
                userRating = ratingAsBigDecimal,
                imdbRating = request.imdbRating?.let { BigDecimal.valueOf(it) }
            )
            watchedMovieRepository.save(watchedMovie)
            logger.info("Movie {} added to watched list with rating {} for user {}", request.movieId, request.rating, username)
            WatchedMovieResult.Success(RateMovieResponse(
                movieId = request.movieId,
                rating = request.rating,
                isNewlyWatched = true,
                message = "Movie added to watched list and rated successfully"
            ))
        }
    }
}
