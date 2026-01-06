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
     * Oyuncunun filmografisini getirir (cache destekli)
     */
    fun getActorFilmography(actorId: Long, limit: Int = 20): ActorFilmographyResponse {
        val cacheKey = CacheKeys.Actor.filmography(actorId)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, ActorFilmographyResponse::class.java)?.let { cached ->
            logger.info("Actor filmography cache HIT for actorId: {}", actorId)
            // Limit uygula
            return cached.copy(movies = cached.movies.take(limit))
        }
        
        // TMDB'den getir - önce actor detayını al (isim için)
        logger.info("Actor filmography cache MISS for actorId: {}, fetching from TMDB", actorId)
        val actorDetail = tmdbApiClient.getActorDetail(actorId)
        val tmdbResponse = tmdbApiClient.getActorMovieCredits(actorId)
        val response = mapToActorFilmographyResponse(actorId, actorDetail.name, tmdbResponse)
        
        // Cache'e yaz (24 saat) - tüm filmleri cache'le
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
        
        // Limit uygula
        return response.copy(movies = response.movies.take(limit))
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
}
