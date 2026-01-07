package com.emirhankarci.moviebackend.tvwatchlist

import com.emirhankarci.moviebackend.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class TvWatchlistService(
    private val tvWatchlistRepository: TvWatchlistRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvWatchlistService::class.java)
    }

    @Transactional
    fun addToWatchlist(user: User, request: TvWatchlistRequest): TvWatchlistResult<TvWatchlistResponse> {
        // Check if already in watchlist
        if (tvWatchlistRepository.existsByUserIdAndSeriesId(user.id!!, request.seriesId)) {
            logger.info("Series {} already in watchlist for user {}", request.seriesId, user.id)
            return TvWatchlistResult.Error("ALREADY_IN_WATCHLIST", "Bu dizi zaten izleme listenizde")
        }

        val watchlistEntry = TvWatchlist(
            user = user,
            seriesId = request.seriesId,
            seriesName = request.seriesName,
            posterPath = request.posterPath,
            voteAverage = request.voteAverage?.let { BigDecimal.valueOf(it) }
        )

        val saved = tvWatchlistRepository.save(watchlistEntry)
        logger.info("Added series {} to watchlist for user {}", request.seriesId, user.id)

        return TvWatchlistResult.Success(saved.toResponse())
    }

    @Transactional
    fun removeFromWatchlist(user: User, seriesId: Long): TvWatchlistResult<Unit> {
        val deleted = tvWatchlistRepository.deleteByUserIdAndSeriesId(user.id!!, seriesId)
        
        return if (deleted > 0) {
            logger.info("Removed series {} from watchlist for user {}", seriesId, user.id)
            TvWatchlistResult.Success(Unit)
        } else {
            logger.info("Series {} not in watchlist for user {}", seriesId, user.id)
            TvWatchlistResult.Error("NOT_IN_WATCHLIST", "Bu dizi izleme listenizde değil")
        }
    }

    fun getWatchlist(user: User, page: Int, size: Int, paginated: Boolean = false): TvWatchlistResult<Any> {
        return if (paginated) {
            val pageable = PageRequest.of(page, size)
            val pageResult = tvWatchlistRepository.findByUserIdOrderByCreatedAtDesc(user.id!!, pageable)
            
            val response = com.emirhankarci.moviebackend.common.PageResponse.from(pageResult) {
                it.toResponse()
            }
            
            TvWatchlistResult.Success(response)
        } else {
            val watchlist = tvWatchlistRepository.findByUserIdOrderByCreatedAtDesc(user.id!!)
                .map { it.toResponse() }
            
            TvWatchlistResult.Success(watchlist)
        }
    }

    fun checkStatus(user: User, seriesId: Long): TvWatchlistStatusResponse {
        val inWatchlist = tvWatchlistRepository.existsByUserIdAndSeriesId(user.id!!, seriesId)
        return TvWatchlistStatusResponse(seriesId, inWatchlist)
    }

    private fun TvWatchlist.toResponse() = TvWatchlistResponse(
        id = this.id!!,
        seriesId = this.seriesId,
        seriesName = this.seriesName,
        posterPath = this.posterPath,
        voteAverage = this.voteAverage?.toDouble(),
        addedAt = this.createdAt
    )
}
