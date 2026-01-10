package com.emirhankarci.moviebackend.tmdb

import com.emirhankarci.moviebackend.cache.CacheKeys
import com.emirhankarci.moviebackend.cache.CacheService
import com.emirhankarci.moviebackend.tmdb.dto.*
import com.emirhankarci.moviebackend.tmdb.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * TMDB Actor Service
 * Cache entegrasyonu ile oyuncu verilerini yöneten servis.
 */
@Service
class TmdbActorService(
    private val tmdbApiClient: TmdbApiClient,
    private val cacheService: CacheService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TmdbActorService::class.java)
    }

    /**
     * Oyuncu detaylarını getirir (cache destekli)
     */
    fun getActorDetail(actorId: Long): ActorDetailResponse {
        val cacheKey = CacheKeys.Actor.detail(actorId)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, ActorDetailResponse::class.java)?.let {
            logger.info("Actor detail cache HIT for actorId: {}", actorId)
            return it
        }
        
        // TMDB'den getir
        logger.info("Actor detail cache MISS for actorId: {}, fetching from TMDB", actorId)
        val tmdbResponse = tmdbApiClient.getActorDetail(actorId)
        val response = mapToActorDetailResponse(tmdbResponse)
        
        // Cache'e yaz (24 saat)
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
        
        return response
    }

    /**
     * Oyuncunun filmografisini getirir (cache destekli, paginated)
     */
    fun getActorFilmography(actorId: Long, page: Int = 1, limit: Int = 20): PaginatedActorFilmographyResponse {
        val cacheKey = CacheKeys.Actor.filmography(actorId)
        
        // Cache'den kontrol et
        val cached = cacheService.get(cacheKey, ActorFilmographyResponse::class.java)
        
        val fullResponse = if (cached != null) {
            logger.info("Actor filmography cache HIT for actorId: {}", actorId)
            cached
        } else {
            // TMDB'den getir - önce actor detayını al (isim için)
            logger.info("Actor filmography cache MISS for actorId: {}, fetching from TMDB", actorId)
            val actorDetail = tmdbApiClient.getActorDetail(actorId)
            val tmdbResponse = tmdbApiClient.getActorMovieCredits(actorId)
            val response = mapToActorFilmographyResponse(actorId, actorDetail.name, tmdbResponse)
            
            // Cache'e yaz (24 saat) - tüm filmleri cache'le
            cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
            response
        }
        
        // Pagination uygula
        val paginatedMovies = fullResponse.movies.paginate(page, limit)
        val paginationInfo = PaginationInfo.calculate(fullResponse.movies.size, page, limit)
        
        return PaginatedActorFilmographyResponse(
            actorId = fullResponse.actorId,
            actorName = fullResponse.actorName,
            movies = paginatedMovies,
            pagination = paginationInfo
        )
    }

    /**
     * Oyuncunun TV dizi kredilerini getirir (cache destekli, paginated)
     */
    fun getActorTvCredits(actorId: Long, page: Int = 1, limit: Int = 20): PaginatedActorTvCreditsResponse {
        val cacheKey = CacheKeys.Actor.tvCredits(actorId)
        
        // Cache'den kontrol et
        val cached = cacheService.get(cacheKey, ActorTvCreditsResponse::class.java)
        
        val fullResponse = if (cached != null) {
            logger.info("Actor TV credits cache HIT for actorId: {}", actorId)
            cached
        } else {
            // TMDB'den getir
            logger.info("Actor TV credits cache MISS for actorId: {}, fetching from TMDB", actorId)
            val actorDetail = tmdbApiClient.getActorDetail(actorId)
            val tmdbResponse = tmdbApiClient.getActorTvCredits(actorId)
            val response = mapToActorTvCreditsResponse(actorId, actorDetail.name, tmdbResponse)
            
            // Cache'e yaz (24 saat)
            cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
            response
        }
        
        // Pagination uygula
        val paginatedTvShows = fullResponse.tvShows.paginate(page, limit)
        val paginationInfo = PaginationInfo.calculate(fullResponse.tvShows.size, page, limit)
        
        return PaginatedActorTvCreditsResponse(
            actorId = fullResponse.actorId,
            actorName = fullResponse.actorName,
            tvShows = paginatedTvShows,
            pagination = paginationInfo
        )
    }

    /**
     * Oyuncunun sosyal medya ID'lerini getirir (cache destekli)
     */
    fun getActorExternalIds(actorId: Long): ActorExternalIdsResponse {
        val cacheKey = CacheKeys.Actor.externalIds(actorId)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, ActorExternalIdsResponse::class.java)?.let {
            logger.info("Actor external IDs cache HIT for actorId: {}", actorId)
            return it
        }
        
        // TMDB'den getir
        logger.info("Actor external IDs cache MISS for actorId: {}, fetching from TMDB", actorId)
        val tmdbResponse = tmdbApiClient.getActorExternalIds(actorId)
        val response = mapToActorExternalIdsResponse(actorId, tmdbResponse)
        
        // Cache'e yaz (24 saat)
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
        
        return response
    }

    // ==================== Mapping Functions ====================

    private fun mapToActorDetailResponse(tmdb: TmdbActorDetailResponse): ActorDetailResponse {
        return ActorDetailResponse(
            id = tmdb.id,
            name = tmdb.name,
            biography = tmdb.biography,
            birthday = tmdb.birthday,
            deathday = tmdb.deathday,
            placeOfBirth = tmdb.place_of_birth,
            profilePath = tmdbApiClient.buildProfileUrl(tmdb.profile_path),
            knownForDepartment = tmdb.known_for_department
        )
    }

    private fun mapToActorFilmographyResponse(
        actorId: Long,
        actorName: String,
        tmdb: TmdbActorMovieCreditsResponse
    ): ActorFilmographyResponse {
        val sortedMovies = tmdb.cast
            .filter { it.release_date != null && it.release_date.isNotBlank() }
            .sortedByDescending { it.release_date }
            .map { credit ->
                ActorMovieCreditDto(
                    id = credit.id,
                    title = credit.title,
                    character = credit.character,
                    posterPath = tmdbApiClient.buildPosterUrl(credit.poster_path),
                    releaseDate = credit.release_date,
                    voteAverage = credit.vote_average
                )
            }
        
        return ActorFilmographyResponse(
            actorId = actorId,
            actorName = actorName,
            movies = sortedMovies
        )
    }

    private fun mapToActorTvCreditsResponse(
        actorId: Long,
        actorName: String,
        tmdb: TmdbActorTvCreditsResponse
    ): ActorTvCreditsResponse {
        val sortedTvShows = tmdb.cast
            .filter { it.first_air_date != null && it.first_air_date.isNotBlank() }
            .sortedByDescending { it.first_air_date }
            .map { credit ->
                ActorTvCreditDto(
                    id = credit.id,
                    name = credit.name,
                    character = credit.character,
                    posterPath = tmdbApiClient.buildPosterUrl(credit.poster_path),
                    firstAirDate = credit.first_air_date,
                    voteAverage = credit.vote_average,
                    episodeCount = credit.episode_count
                )
            }
        
        return ActorTvCreditsResponse(
            actorId = actorId,
            actorName = actorName,
            tvShows = sortedTvShows
        )
    }

    private fun mapToActorExternalIdsResponse(
        actorId: Long,
        tmdb: TmdbActorExternalIdsResponse
    ): ActorExternalIdsResponse {
        return ActorExternalIdsResponse(
            actorId = actorId,
            instagramId = tmdb.instagram_id,
            twitterId = tmdb.twitter_id,
            instagramUrl = tmdb.instagram_id?.let { "https://instagram.com/$it" },
            twitterUrl = tmdb.twitter_id?.let { "https://twitter.com/$it" }
        )
    }
}
