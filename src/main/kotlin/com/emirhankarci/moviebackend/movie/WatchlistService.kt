package com.emirhankarci.moviebackend.movie

import com.emirhankarci.moviebackend.common.ImageUrlBuilder
import com.emirhankarci.moviebackend.common.PageResponse
import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class WatchlistService(
    private val watchlistRepository: WatchlistRepository,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WatchlistService::class.java)
    }

    @Transactional
    fun toggleWatchlist(username: String, request: WatchlistRequest): WatchlistResult<String> {
        val user = userRepository.findByUsername(username)
            ?: return WatchlistResult.Error("User not found!")

        val existing = watchlistRepository.findByUserIdAndMovieId(user.id!!, request.movieId)

        return if (existing.isPresent) {
            watchlistRepository.delete(existing.get())
            logger.info("Movie {} removed from watchlist for user {}", request.movieId, username)
            WatchlistResult.Success("Movie removed from watchlist")
        } else {
            val watchlistEntry = Watchlist(
                user = user,
                movieId = request.movieId,
                movieTitle = request.movieTitle,
                posterPath = request.posterPath,
                imdbRating = request.imdbRating?.let { BigDecimal.valueOf(it) }
            )
            watchlistRepository.save(watchlistEntry)
            logger.info("Movie {} added to watchlist for user {}", request.movieId, username)
            WatchlistResult.Success("Movie added to watchlist")
        }
    }

    fun isMovieInWatchlist(username: String, movieId: Long): WatchlistResult<WatchlistStatusResponse> {
        val user = userRepository.findByUsername(username)
            ?: return WatchlistResult.Error("User not found!")

        val exists = watchlistRepository.existsByUserIdAndMovieId(user.id!!, movieId)
        logger.debug("Watchlist status check for user {}, movie {}: {}", username, movieId, exists)
        
        return WatchlistResult.Success(WatchlistStatusResponse(movieId, exists))
    }

    @Transactional
    fun removeFromWatchlist(username: String, movieId: Long): WatchlistResult<String> {
        val user = userRepository.findByUsername(username)
            ?: return WatchlistResult.Error("User not found!")

        val existing = watchlistRepository.findByUserIdAndMovieId(user.id!!, movieId)
        
        return if (existing.isPresent) {
            watchlistRepository.delete(existing.get())
            logger.info("Movie {} removed from watchlist for user {}", movieId, username)
            WatchlistResult.Success("Movie removed from watchlist")
        } else {
            logger.warn("Attempted to remove non-existent movie {} from watchlist for user {}", movieId, username)
            WatchlistResult.Error("Movie not found in watchlist")
        }
    }

    fun getUserWatchlist(
        username: String,
        page: Int = 0,
        size: Int = 20,
        paginated: Boolean = false,
        sortOrder: String = "desc"
    ): WatchlistResult<Any> {
        val user = userRepository.findByUsername(username)
            ?: return WatchlistResult.Error("User not found!")

        return if (paginated) {
            val direction = if (sortOrder.equals("asc", ignoreCase = true)) org.springframework.data.domain.Sort.Direction.ASC else org.springframework.data.domain.Sort.Direction.DESC
            val pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, "createdAt"))
            val pageResult = watchlistRepository.findByUserId(user.id!!, pageable)
            
            val response = PageResponse.from(pageResult) {
                WatchlistResponse(
                    id = it.id!!,
                    movieId = it.movieId,
                    movieTitle = it.movieTitle,
                    posterPath = ImageUrlBuilder.buildPosterUrl(it.posterPath),
                    addedAt = it.createdAt,
                    imdbRating = it.imdbRating?.toDouble()
                )
            }
            
            logger.debug("Returning paginated watchlist for user {}: page {}, size {}, sort {}", username, page, size, sortOrder)
            WatchlistResult.Success(response)
        } else {
            val watchlist = watchlistRepository.findByUserIdOrderByCreatedAtDesc(user.id!!)
                .map {
                    WatchlistResponse(
                        id = it.id!!,
                        movieId = it.movieId,
                        movieTitle = it.movieTitle,
                        posterPath = ImageUrlBuilder.buildPosterUrl(it.posterPath),
                        addedAt = it.createdAt,
                        imdbRating = it.imdbRating?.toDouble()
                    )
                }
            
            logger.debug("Returning full watchlist for user {}: {} items", username, watchlist.size)
            WatchlistResult.Success(watchlist)
        }
    }
}
