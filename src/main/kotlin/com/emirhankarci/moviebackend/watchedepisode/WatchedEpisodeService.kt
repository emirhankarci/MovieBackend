package com.emirhankarci.moviebackend.watchedepisode

import com.emirhankarci.moviebackend.user.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import com.emirhankarci.moviebackend.tvseries.TvSeriesService
import kotlinx.coroutines.*

@Service
class WatchedEpisodeService(
    private val watchedEpisodeRepository: WatchedEpisodeRepository,
    private val tvSeriesService: TvSeriesService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WatchedEpisodeService::class.java)
    }

    @Transactional
    fun markEpisodeWatched(user: User, request: MarkEpisodeWatchedRequest): WatchedEpisodeResult<WatchedEpisodeDto> {
        val exists = watchedEpisodeRepository.existsByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
            user.id!!, request.seriesId, request.seasonNumber, request.episodeNumber
        )
        
        if (exists) {
            logger.info("Episode S{}E{} of series {} already watched for user {}", 
                request.seasonNumber, request.episodeNumber, request.seriesId, user.id)
            return WatchedEpisodeResult.Error("ALREADY_WATCHED", "Bu bölüm zaten izlendi olarak işaretli")
        }

        val episode = WatchedEpisode(
            user = user,
            seriesId = request.seriesId,
            seriesName = request.seriesName,
            seasonNumber = request.seasonNumber,
            episodeNumber = request.episodeNumber,
            episodeName = request.episodeName
        )

        val saved = watchedEpisodeRepository.save(episode)
        logger.info("Marked S{}E{} of series {} as watched for user {}", 
            request.seasonNumber, request.episodeNumber, request.seriesId, user.id)

        return WatchedEpisodeResult.Success(saved.toDto())
    }

    @Transactional
    fun unmarkEpisodeWatched(user: User, request: UnmarkEpisodeRequest): WatchedEpisodeResult<Unit> {
        val deleted = watchedEpisodeRepository.deleteByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
            user.id!!, request.seriesId, request.seasonNumber, request.episodeNumber
        )

        return if (deleted > 0) {
            logger.info("Unmarked S{}E{} of series {} for user {}", 
                request.seasonNumber, request.episodeNumber, request.seriesId, user.id)
            WatchedEpisodeResult.Success(Unit)
        } else {
            WatchedEpisodeResult.Error("NOT_WATCHED", "Bu bölüm izlendi olarak işaretli değil")
        }
    }

    @Transactional
    fun markSeasonWatched(user: User, request: MarkSeasonWatchedRequest): WatchedEpisodeResult<Int> {
        var addedCount = 0

        request.episodes.forEach { episodeInfo ->
            val exists = watchedEpisodeRepository.existsByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
                user.id!!, request.seriesId, request.seasonNumber, episodeInfo.episodeNumber
            )
            
            if (!exists) {
                val episode = WatchedEpisode(
                    user = user,
                    seriesId = request.seriesId,
                    seriesName = request.seriesName,
                    seasonNumber = request.seasonNumber,
                    episodeNumber = episodeInfo.episodeNumber,
                    episodeName = episodeInfo.episodeName
                )
                watchedEpisodeRepository.save(episode)
                addedCount++
            }
        }

        logger.info("Marked {} episodes of season {} for series {} as watched for user {}", 
            addedCount, request.seasonNumber, request.seriesId, user.id)
        return WatchedEpisodeResult.Success(addedCount)
    }

    @Transactional
    fun unmarkSeasonWatched(user: User, request: UnmarkSeasonRequest): WatchedEpisodeResult<Int> {
        val deleted = watchedEpisodeRepository.deleteByUserIdAndSeriesIdAndSeasonNumber(
            user.id!!, request.seriesId, request.seasonNumber
        )

        logger.info("Unmarked {} episodes of season {} for series {} for user {}", 
            deleted, request.seasonNumber, request.seriesId, user.id)
        return WatchedEpisodeResult.Success(deleted)
    }

    fun getWatchedEpisodes(user: User, seriesId: Long): WatchedEpisodesResponse? {
        val episodes = watchedEpisodeRepository.findByUserIdAndSeriesId(user.id!!, seriesId)
        
        if (episodes.isEmpty()) return null

        val seriesName = episodes.first().seriesName
        val seasons = episodes
            .groupBy { it.seasonNumber }
            .map { (seasonNumber, seasonEpisodes) ->
                WatchedSeasonDto(
                    seasonNumber = seasonNumber,
                    episodes = seasonEpisodes
                        .sortedBy { it.episodeNumber }
                        .map { it.toDto() }
                )
            }
            .sortedBy { it.seasonNumber }

        return WatchedEpisodesResponse(seriesId, seriesName, seasons)
    }

    fun getWatchProgress(user: User, seriesId: Long, clientProvidedCounts: Map<Int, Int>? = null): WatchProgressResponse {
        // 1. Get watched episodes from DB
        val watchedEpisodes = watchedEpisodeRepository.findByUserIdAndSeriesId(user.id!!, seriesId)

        // 2. Fetch series details to get season list
        val seriesDetail = tvSeriesService.getSeriesDetail(seriesId)
        
        // 3. Fetch season details in parallel to get accurate episode counts
        val seasonDetails = runBlocking(Dispatchers.IO) {
            seriesDetail.seasons.map { seasonSummary ->
                async {
                    try {
                        tvSeriesService.getSeasonDetail(seriesId, seasonSummary.seasonNumber)
                    } catch (e: Exception) {
                        logger.error("Failed to fetch season detail for series {} season {}", seriesId, seasonSummary.seasonNumber, e)
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        // 4. Calculate stats per season
        val seasonProgressList = seasonDetails.map { season ->
            val totalEpisodes = season.episodes.size
            // Count watched episodes for this specific season
            // We use count { ... } to ensure we only count episodes that actually exist in the season
            val watchedInSeason = watchedEpisodes.filter { 
                it.seasonNumber == season.seasonNumber && 
                season.episodes.any { ep -> ep.episodeNumber == it.episodeNumber }
            }.size
            
            SeasonProgressDto(
                seasonNumber = season.seasonNumber,
                totalEpisodes = totalEpisodes,
                watchedEpisodes = watchedInSeason,
                progressPercentage = if (totalEpisodes > 0) 
                    (watchedInSeason.toDouble() / totalEpisodes * 100).roundTo(1) else 0.0
            )
        }.sortedBy { it.seasonNumber }

        // 5. Calculate total stats
        val totalEpisodes = seasonProgressList.sumOf { it.totalEpisodes }
        val grandTotalWatched = seasonProgressList.sumOf { it.watchedEpisodes }

        return WatchProgressResponse(
            seriesId = seriesId,
            totalEpisodes = totalEpisodes,
            watchedEpisodes = grandTotalWatched,
            progressPercentage = if (totalEpisodes > 0) 
                (grandTotalWatched.toDouble() / totalEpisodes * 100).roundTo(1) else 0.0,
            seasonProgress = seasonProgressList
        )
    }


    fun getWatchedSeries(user: User, page: Int, size: Int): com.emirhankarci.moviebackend.common.PageResponse<WatchedSeriesDto> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val watchedSeriesPage = watchedEpisodeRepository.findWatchedSeriesByUserId(user.id!!, pageable)
        
        // Fetch details for all series in the page in parallel to get images
        val enrichedContent = runBlocking(Dispatchers.IO) {
            watchedSeriesPage.content.map { summary ->
                async {
                    try {
                        val details = tvSeriesService.getSeriesDetail(summary.seriesId)
                        WatchedSeriesDto(
                            seriesId = summary.seriesId,
                            seriesName = summary.seriesName,
                            lastWatchedAt = summary.lastWatchedAt,
                            posterPath = details.posterPath,
                            backdropPath = details.backdropPath
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to fetch details for series {}", summary.seriesId, e)
                        // Fallback to basic info if fetch fails
                        WatchedSeriesDto(
                            seriesId = summary.seriesId,
                            seriesName = summary.seriesName,
                            lastWatchedAt = summary.lastWatchedAt
                        )
                    }
                }
            }.awaitAll()
        }
        
        return com.emirhankarci.moviebackend.common.PageResponse(
            content = enrichedContent,
            page = watchedSeriesPage.number,
            size = watchedSeriesPage.size,
            totalElements = watchedSeriesPage.totalElements,
            totalPages = watchedSeriesPage.totalPages,
            hasNext = watchedSeriesPage.hasNext(),
            hasPrevious = watchedSeriesPage.hasPrevious()
        )
    }

    fun checkEpisodeStatus(user: User, seriesId: Long, seasonNumber: Int, episodeNumber: Int): EpisodeWatchStatusResponse {
        val isWatched = watchedEpisodeRepository.existsByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
            user.id!!, seriesId, seasonNumber, episodeNumber
        )
        return EpisodeWatchStatusResponse(seriesId, seasonNumber, episodeNumber, isWatched)
    }

    // Helper to mark episode as watched (used by rating service for auto-watch)
    @Transactional
    fun ensureEpisodeWatched(user: User, seriesId: Long, seriesName: String, 
                             seasonNumber: Int, episodeNumber: Int, episodeName: String?) {
        val exists = watchedEpisodeRepository.existsByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
            user.id!!, seriesId, seasonNumber, episodeNumber
        )
        
        if (!exists) {
            val episode = WatchedEpisode(
                user = user,
                seriesId = seriesId,
                seriesName = seriesName,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeName = episodeName
            )
            watchedEpisodeRepository.save(episode)
            logger.info("Auto-marked S{}E{} of series {} as watched for user {}", 
                seasonNumber, episodeNumber, seriesId, user.id)
        }
    }

    private fun WatchedEpisode.toDto() = WatchedEpisodeDto(
        episodeNumber = this.episodeNumber,
        episodeName = this.episodeName,
        watchedAt = this.watchedAt
    )

    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}
