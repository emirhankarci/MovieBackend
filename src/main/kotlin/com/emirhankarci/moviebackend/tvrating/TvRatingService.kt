package com.emirhankarci.moviebackend.tvrating

import com.emirhankarci.moviebackend.user.User
import com.emirhankarci.moviebackend.watchedepisode.WatchedEpisodeService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class TvRatingService(
    private val tvSeriesRatingRepository: TvSeriesRatingRepository,
    private val episodeRatingRepository: EpisodeRatingRepository,
    private val watchedEpisodeService: WatchedEpisodeService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvRatingService::class.java)
        private const val MIN_RATING = 1.0
        private const val MAX_RATING = 10.0
    }

    // Series Rating Methods
    @Transactional
    fun rateSeries(user: User, request: RateSeriesRequest): TvRatingResult<RatedSeriesResponse> {
        if (!isValidRating(request.rating)) {
            return TvRatingResult.Error("INVALID_RATING", "Puan 1.0 ile 10.0 arasında olmalıdır")
        }

        val existing = tvSeriesRatingRepository.findByUserIdAndSeriesId(user.id!!, request.seriesId)
        
        return if (existing != null) {
            // Update existing rating
            val updated = existing.copy(
                rating = BigDecimal.valueOf(request.rating),
                posterPath = request.posterPath ?: existing.posterPath
            )
            val saved = tvSeriesRatingRepository.save(updated)
            logger.info("Updated series {} rating to {} for user {}", request.seriesId, request.rating, user.id)
            TvRatingResult.Success(saved.toResponse())
        } else {
            // Create new rating
            val rating = TvSeriesRating(
                user = user,
                seriesId = request.seriesId,
                seriesName = request.seriesName,
                posterPath = request.posterPath,
                rating = BigDecimal.valueOf(request.rating)
            )
            val saved = tvSeriesRatingRepository.save(rating)
            logger.info("Rated series {} with {} for user {}", request.seriesId, request.rating, user.id)
            TvRatingResult.Success(saved.toResponse())
        }
    }

    @Transactional
    fun removeSeriesRating(user: User, seriesId: Long): TvRatingResult<Unit> {
        val deleted = tvSeriesRatingRepository.deleteByUserIdAndSeriesId(user.id!!, seriesId)
        
        return if (deleted > 0) {
            logger.info("Removed series {} rating for user {}", seriesId, user.id)
            TvRatingResult.Success(Unit)
        } else {
            TvRatingResult.Error("NOT_RATED", "Bu dizi puanlanmamış")
        }
    }

    fun getRatedSeries(user: User, page: Int, size: Int): Page<RatedSeriesResponse> {
        val pageable = PageRequest.of(page, size)
        return tvSeriesRatingRepository.findByUserIdOrderByRatedAtDesc(user.id!!, pageable)
            .map { it.toResponse() }
    }

    fun getSeriesRatingStatus(user: User, seriesId: Long): SeriesRatingStatusResponse {
        val rating = tvSeriesRatingRepository.findByUserIdAndSeriesId(user.id!!, seriesId)
        return SeriesRatingStatusResponse(
            seriesId = seriesId,
            isRated = rating != null,
            rating = rating?.rating?.toDouble()
        )
    }

    // Episode Rating Methods
    @Transactional
    fun rateEpisode(user: User, request: RateEpisodeRequest): TvRatingResult<RatedEpisodeResponse> {
        if (!isValidRating(request.rating)) {
            return TvRatingResult.Error("INVALID_RATING", "Puan 1.0 ile 10.0 arasında olmalıdır")
        }

        val existing = episodeRatingRepository.findByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
            user.id!!, request.seriesId, request.seasonNumber, request.episodeNumber
        )

        // Auto-watch the episode when rating
        watchedEpisodeService.ensureEpisodeWatched(
            user, request.seriesId, request.seriesName,
            request.seasonNumber, request.episodeNumber, request.episodeName
        )

        return if (existing != null) {
            // Update existing rating
            val updated = existing.copy(
                rating = BigDecimal.valueOf(request.rating),
                episodeName = request.episodeName ?: existing.episodeName
            )
            val saved = episodeRatingRepository.save(updated)
            logger.info("Updated S{}E{} rating to {} for user {}", 
                request.seasonNumber, request.episodeNumber, request.rating, user.id)
            TvRatingResult.Success(saved.toResponse())
        } else {
            // Create new rating
            val rating = EpisodeRating(
                user = user,
                seriesId = request.seriesId,
                seriesName = request.seriesName,
                seasonNumber = request.seasonNumber,
                episodeNumber = request.episodeNumber,
                episodeName = request.episodeName,
                rating = BigDecimal.valueOf(request.rating)
            )
            val saved = episodeRatingRepository.save(rating)
            logger.info("Rated S{}E{} with {} for user {}", 
                request.seasonNumber, request.episodeNumber, request.rating, user.id)
            TvRatingResult.Success(saved.toResponse())
        }
    }

    @Transactional
    fun removeEpisodeRating(user: User, request: DeleteEpisodeRatingRequest): TvRatingResult<Unit> {
        val deleted = episodeRatingRepository.deleteByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
            user.id!!, request.seriesId, request.seasonNumber, request.episodeNumber
        )

        return if (deleted > 0) {
            logger.info("Removed S{}E{} rating for user {}", 
                request.seasonNumber, request.episodeNumber, user.id)
            TvRatingResult.Success(Unit)
        } else {
            TvRatingResult.Error("NOT_RATED", "Bu bölüm puanlanmamış")
        }
    }

    fun getRatedEpisodes(user: User, seriesId: Long): RatedEpisodesResponse {
        val ratings = episodeRatingRepository.findByUserIdAndSeriesId(user.id!!, seriesId)
        return RatedEpisodesResponse(
            seriesId = seriesId,
            episodes = ratings.map { it.toResponse() }
        )
    }

    private fun isValidRating(rating: Double): Boolean = rating in MIN_RATING..MAX_RATING

    private fun TvSeriesRating.toResponse() = RatedSeriesResponse(
        id = this.id!!,
        seriesId = this.seriesId,
        seriesName = this.seriesName,
        posterPath = this.posterPath,
        rating = this.rating.toDouble(),
        ratedAt = this.ratedAt
    )

    private fun EpisodeRating.toResponse() = RatedEpisodeResponse(
        id = this.id!!,
        seriesId = this.seriesId,
        seasonNumber = this.seasonNumber,
        episodeNumber = this.episodeNumber,
        episodeName = this.episodeName,
        rating = this.rating.toDouble(),
        ratedAt = this.ratedAt
    )
}
