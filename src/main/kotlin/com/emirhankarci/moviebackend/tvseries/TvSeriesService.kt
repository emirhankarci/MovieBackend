package com.emirhankarci.moviebackend.tvseries

import com.emirhankarci.moviebackend.cache.CacheKeys
import com.emirhankarci.moviebackend.cache.CacheService
import com.emirhankarci.moviebackend.tmdb.TmdbApiClient
import com.emirhankarci.moviebackend.tmdb.dto.GenreDto
import com.emirhankarci.moviebackend.tmdb.model.*
import com.emirhankarci.moviebackend.tvseries.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * TV Series Service
 * Cache entegrasyonu ile TV dizisi verilerini yöneten servis.
 */
@Service
class TvSeriesService(
    private val tmdbApiClient: TmdbApiClient,
    private val cacheService: CacheService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvSeriesService::class.java)
        private const val DEFAULT_CAST_LIMIT = 20
    }

    /**
     * TV dizisi detaylarını getirir (cache destekli)
     */
    fun getSeriesDetail(seriesId: Long): TvSeriesDetailResponse {
        val cacheKey = CacheKeys.TvSeries.detail(seriesId)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, TvSeriesDetailResponse::class.java)?.let {
            logger.info("TV series detail cache HIT for seriesId: {}", seriesId)
            return it
        }
        
        // TMDB'den getir
        logger.info("TV series detail cache MISS for seriesId: {}, fetching from TMDB", seriesId)
        val tmdbResponse = tmdbApiClient.getTvSeriesDetail(seriesId)
        val response = mapToTvSeriesDetailResponse(tmdbResponse)
        
        // Cache'e yaz (24 saat)
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
        
        return response
    }

    /**
     * Sezon detaylarını getirir (cache destekli)
     */
    fun getSeasonDetail(seriesId: Long, seasonNumber: Int): SeasonDetailResponse {
        val cacheKey = CacheKeys.TvSeries.season(seriesId, seasonNumber)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, SeasonDetailResponse::class.java)?.let {
            logger.info("Season detail cache HIT for seriesId: {}, season: {}", seriesId, seasonNumber)
            return it
        }
        
        // TMDB'den getir
        logger.info("Season detail cache MISS for seriesId: {}, season: {}, fetching from TMDB", seriesId, seasonNumber)
        val tmdbResponse = tmdbApiClient.getTvSeasonDetail(seriesId, seasonNumber)
        val response = mapToSeasonDetailResponse(seriesId, tmdbResponse)
        
        // Cache'e yaz (24 saat)
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
        
        return response
    }


    /**
     * Bölüm detaylarını getirir (cache destekli)
     */
    fun getEpisodeDetail(seriesId: Long, seasonNumber: Int, episodeNumber: Int): EpisodeDetailResponse {
        val cacheKey = CacheKeys.TvSeries.episode(seriesId, seasonNumber, episodeNumber)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, EpisodeDetailResponse::class.java)?.let {
            logger.info("Episode detail cache HIT for seriesId: {}, season: {}, episode: {}", seriesId, seasonNumber, episodeNumber)
            return it
        }
        
        // TMDB'den getir
        logger.info("Episode detail cache MISS for seriesId: {}, season: {}, episode: {}, fetching from TMDB", seriesId, seasonNumber, episodeNumber)
        val tmdbResponse = tmdbApiClient.getTvEpisodeDetail(seriesId, seasonNumber, episodeNumber)
        val response = mapToEpisodeDetailResponse(seriesId, tmdbResponse)
        
        // Cache'e yaz (24 saat)
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
        
        return response
    }

    /**
     * TV dizisi kadrosunu getirir (cache destekli)
     */
    fun getSeriesCredits(seriesId: Long, limit: Int = DEFAULT_CAST_LIMIT): TvSeriesCreditsResponse {
        val cacheKey = CacheKeys.TvSeries.credits(seriesId)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, TvSeriesCreditsResponse::class.java)?.let { cached ->
            logger.info("TV series credits cache HIT for seriesId: {}", seriesId)
            // Limit uygula
            return cached.copy(cast = cached.cast.take(limit))
        }
        
        // TMDB'den getir
        logger.info("TV series credits cache MISS for seriesId: {}, fetching from TMDB", seriesId)
        val tmdbResponse = tmdbApiClient.getTvSeriesCredits(seriesId)
        val response = mapToTvSeriesCreditsResponse(seriesId, tmdbResponse)
        
        // Cache'e yaz (24 saat) - tüm cast'i cache'le
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
        
        // Limit uygula
        return response.copy(cast = response.cast.take(limit))
    }

    // ==================== Mapping Functions ====================

    private fun mapToTvSeriesDetailResponse(tmdb: TmdbTvSeriesDetailResponse): TvSeriesDetailResponse {
        return TvSeriesDetailResponse(
            id = tmdb.id,
            name = tmdb.name,
            overview = tmdb.overview ?: "",
            posterPath = tmdbApiClient.buildPosterUrl(tmdb.poster_path),
            backdropPath = tmdbApiClient.buildBackdropUrl(tmdb.backdrop_path),
            firstAirDate = tmdb.first_air_date,
            lastAirDate = tmdb.last_air_date,
            status = tmdb.status ?: "Unknown",
            tagline = tmdb.tagline,
            voteAverage = tmdb.vote_average,
            voteCount = tmdb.vote_count,
            numberOfSeasons = tmdb.number_of_seasons,
            numberOfEpisodes = tmdb.number_of_episodes,
            episodeRunTime = tmdb.episode_run_time ?: emptyList(),
            genres = tmdb.genres.map { GenreDto(it.id, it.name) },
            networks = tmdb.networks?.map { network ->
                NetworkDto(
                    id = network.id,
                    name = network.name,
                    logoPath = tmdbApiClient.buildPosterUrl(network.logo_path)
                )
            } ?: emptyList(),
            seasons = tmdb.seasons?.map { season ->
                SeasonSummaryDto(
                    id = season.id,
                    seasonNumber = season.season_number,
                    name = season.name ?: "Season ${season.season_number}",
                    episodeCount = season.episode_count,
                    airDate = season.air_date,
                    posterPath = tmdbApiClient.buildPosterUrl(season.poster_path)
                )
            } ?: emptyList()
        )
    }


    private fun mapToSeasonDetailResponse(seriesId: Long, tmdb: TmdbSeasonDetailResponse): SeasonDetailResponse {
        return SeasonDetailResponse(
            id = tmdb.id,
            seriesId = seriesId,
            seasonNumber = tmdb.season_number,
            name = tmdb.name ?: "Season ${tmdb.season_number}",
            overview = tmdb.overview,
            airDate = tmdb.air_date,
            posterPath = tmdbApiClient.buildPosterUrl(tmdb.poster_path),
            episodes = tmdb.episodes?.map { episode ->
                EpisodeSummaryDto(
                    id = episode.id,
                    episodeNumber = episode.episode_number,
                    name = episode.name ?: "Episode ${episode.episode_number}",
                    overview = episode.overview,
                    airDate = episode.air_date,
                    stillPath = tmdbApiClient.buildStillUrl(episode.still_path),
                    voteAverage = episode.vote_average,
                    runtime = episode.runtime
                )
            } ?: emptyList()
        )
    }

    private fun mapToEpisodeDetailResponse(seriesId: Long, tmdb: TmdbEpisodeDetailResponse): EpisodeDetailResponse {
        return EpisodeDetailResponse(
            id = tmdb.id,
            seriesId = seriesId,
            seasonNumber = tmdb.season_number,
            episodeNumber = tmdb.episode_number,
            name = tmdb.name ?: "Episode ${tmdb.episode_number}",
            overview = tmdb.overview,
            airDate = tmdb.air_date,
            stillPath = tmdbApiClient.buildStillUrl(tmdb.still_path),
            voteAverage = tmdb.vote_average,
            voteCount = tmdb.vote_count,
            runtime = tmdb.runtime,
            guestStars = tmdb.guest_stars?.map { guest ->
                GuestStarDto(
                    id = guest.id,
                    name = guest.name,
                    character = guest.character ?: "",
                    profilePath = tmdbApiClient.buildProfileUrl(guest.profile_path)
                )
            } ?: emptyList()
        )
    }

    private fun mapToTvSeriesCreditsResponse(seriesId: Long, tmdb: TmdbTvCreditsResponse): TvSeriesCreditsResponse {
        return TvSeriesCreditsResponse(
            seriesId = seriesId,
            cast = tmdb.cast
                ?.sortedBy { it.order ?: Int.MAX_VALUE }
                ?.map { cast ->
                    TvCastMemberDto(
                        id = cast.id,
                        name = cast.name,
                        character = cast.character ?: "",
                        profilePath = tmdbApiClient.buildProfileUrl(cast.profile_path),
                        order = cast.order ?: Int.MAX_VALUE
                    )
                } ?: emptyList(),
            crew = tmdb.crew
                ?.filter { it.department in listOf("Directing", "Writing", "Production") }
                ?.map { crew ->
                    TvCrewMemberDto(
                        id = crew.id,
                        name = crew.name,
                        job = crew.job ?: "",
                        department = crew.department ?: "",
                        profilePath = tmdbApiClient.buildProfileUrl(crew.profile_path)
                    )
                } ?: emptyList()
        )
    }
}
