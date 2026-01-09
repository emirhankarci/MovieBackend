package com.emirhankarci.moviebackend.tmdb

import com.emirhankarci.moviebackend.cache.CacheKeys
import com.emirhankarci.moviebackend.cache.CacheService
import com.emirhankarci.moviebackend.tmdb.dto.*
import com.emirhankarci.moviebackend.tmdb.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * TMDB Movie Service
 * Cache entegrasyonu ile film verilerini yöneten servis.
 */
@Service
class TmdbMovieService(
    private val tmdbApiClient: TmdbApiClient,
    private val cacheService: CacheService,
    private val upcomingQualityFilter: UpcomingMoviesQualityFilter
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TmdbMovieService::class.java)
    }

    /**
     * Popüler filmleri getirir (cache destekli)
     */
    fun getPopularMovies(page: Int = 1): PopularMoviesResponse {
        val cacheKey = CacheKeys.Movie.popular(page)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, PopularMoviesResponse::class.java)?.let {
            logger.info("Popular movies cache HIT for page: {}", page)
            return it
        }
        
        // TMDB'den getir
        logger.info("Popular movies cache MISS for page: {}, fetching from TMDB", page)
        val tmdbResponse = tmdbApiClient.getPopularMovies(page)
        val response = mapToPopularMoviesResponse(tmdbResponse, page)
        
        // Cache'e yaz (1 saat)
        cacheService.set(cacheKey, response, CacheKeys.TTL.SHORT)
        
        return response
    }

    /**
     * Film detaylarını getirir (cache destekli)
     */
    fun getMovieDetail(movieId: Long): MovieDetailResponse {
        val cacheKey = CacheKeys.Movie.detail(movieId)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, MovieDetailResponse::class.java)?.let {
            logger.info("Movie detail cache HIT for movieId: {}", movieId)
            return it
        }
        
        // TMDB'den getir
        logger.info("Movie detail cache MISS for movieId: {}, fetching from TMDB", movieId)
        val tmdbResponse = tmdbApiClient.getMovieDetail(movieId)
        val response = mapToMovieDetailResponse(tmdbResponse)
        
        // Cache'e yaz (24 saat)
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
        
        return response
    }

    /**
     * Film kadrosunu getirir (cache destekli)
     */
    fun getMovieCredits(movieId: Long, limit: Int = 20): MovieCreditsResponse {
        val cacheKey = CacheKeys.Movie.credits(movieId)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, MovieCreditsResponse::class.java)?.let { cached ->
            logger.info("Movie credits cache HIT for movieId: {}", movieId)
            // Limit uygula
            return cached.copy(cast = cached.cast.take(limit))
        }
        
        // TMDB'den getir
        logger.info("Movie credits cache MISS for movieId: {}, fetching from TMDB", movieId)
        val tmdbResponse = tmdbApiClient.getMovieCredits(movieId)
        val response = mapToMovieCreditsResponse(movieId, tmdbResponse)
        
        // Cache'e yaz (24 saat) - tüm cast'i cache'le
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)
        
        // Limit uygula
        return response.copy(cast = response.cast.take(limit))
    }

    /**
     * Film önerilerini getirir (cache destekli)
     */
    fun getMovieRecommendations(movieId: Long, limit: Int = 10): MovieRecommendationsResponse {
        val cacheKey = CacheKeys.Movie.recommendations(movieId)
        
        // Cache'den kontrol et
        cacheService.get(cacheKey, MovieRecommendationsResponse::class.java)?.let { cached ->
            logger.info("Movie recommendations cache HIT for movieId: {}", movieId)
            // Limit uygula
            return cached.copy(recommendations = cached.recommendations.take(limit))
        }
        
        // TMDB'den getir
        logger.info("Movie recommendations cache MISS for movieId: {}, fetching from TMDB", movieId)
        val tmdbResponse = tmdbApiClient.getMovieRecommendations(movieId)
        val response = mapToMovieRecommendationsResponse(movieId, tmdbResponse)
        
        // Cache'e yaz (6 saat) - tüm önerileri cache'le
        cacheService.set(cacheKey, response, CacheKeys.TTL.MEDIUM)
        
        // Limit uygula
        return response.copy(recommendations = response.recommendations.take(limit))
    }

    /**
     * Vizyondaki filmleri getirir (cache destekli)
     */
    fun getNowPlayingMovies(page: Int = 1): PopularMoviesResponse {
        val cacheKey = CacheKeys.Movie.nowPlaying(page)

        // Cache'den kontrol et
        cacheService.get(cacheKey, PopularMoviesResponse::class.java)?.let {
            logger.info("Now playing movies cache HIT for page: {}", page)
            return it
        }

        // TMDB'den getir
        logger.info("Now playing movies cache MISS for page: {}, fetching from TMDB", page)
        val tmdbResponse = tmdbApiClient.getNowPlayingMovies(page)
        val response = mapToPopularMoviesResponse(tmdbResponse, page)

        // Cache'e yaz (1 saat)
        cacheService.set(cacheKey, response, CacheKeys.TTL.SHORT)

        return response
    }

    /**
     * Gelecek filmleri getirir (cache destekli, kalite filtrelemeli)
     * Discover API kullanarak bugünden 3 ay sonrasına kadar olan filmleri döndürür.
     * API seviyesinde ve uygulama seviyesinde kalite filtrelemesi uygular.
     */
    fun getUpcomingMovies(page: Int = 1): PopularMoviesResponse {
        val cacheKey = CacheKeys.Movie.upcoming(page)

        // Cache'den kontrol et
        cacheService.get(cacheKey, PopularMoviesResponse::class.java)?.let {
            logger.info("Upcoming movies cache HIT for page: {}", page)
            return it
        }

        // API filtre parametrelerini al
        val filterParams = upcomingQualityFilter.getApiFilterParams()
        
        // TMDB'den getir (Discover API ile 90 gün ileriye bakar + API filtreleri)
        logger.info("Upcoming movies cache MISS for page: {}, fetching from TMDB with quality filters", page)
        val tmdbResponse = tmdbApiClient.getUpcomingMovies(page, filterParams = filterParams)
        
        // Uygulama seviyesi filtreleme (poster ve popülerlik kontrolü)
        val filteredResults = upcomingQualityFilter.filter(tmdbResponse.results)
        
        if (filteredResults.isEmpty() && tmdbResponse.results.isNotEmpty()) {
            logger.warn("All {} upcoming movies were filtered out for page: {}", tmdbResponse.results.size, page)
        } else if (filteredResults.size < tmdbResponse.results.size) {
            logger.debug("Filtered {} out of {} upcoming movies for page: {}", 
                tmdbResponse.results.size - filteredResults.size, tmdbResponse.results.size, page)
        }
        
        // Filtrelenmiş response oluştur
        val filteredTmdbResponse = tmdbResponse.copy(results = filteredResults)
        val response = mapToPopularMoviesResponse(filteredTmdbResponse, page)

        // Cache'e yaz (1 saat)
        cacheService.set(cacheKey, response, CacheKeys.TTL.SHORT)

        return response
    }

    /**
     * En yüksek puanlı filmleri getirir (cache destekli)
     */
    fun getTopRatedMovies(page: Int = 1): PopularMoviesResponse {
        val cacheKey = CacheKeys.Movie.topRated(page)

        // Cache'den kontrol et
        cacheService.get(cacheKey, PopularMoviesResponse::class.java)?.let {
            logger.info("Top rated movies cache HIT for page: {}", page)
            return it
        }

        // TMDB'den getir
        logger.info("Top rated movies cache MISS for page: {}, fetching from TMDB", page)
        val tmdbResponse = tmdbApiClient.getTopRatedMovies(page)
        val response = mapToPopularMoviesResponse(tmdbResponse, page)

        // Cache'e yaz (24 saat - çok sık değişmez)
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)

        return response
    }

    /**
     * Film yaş sınırını/sertifikasını getirir (cache destekli)
     * Öncelik TR, yoksa US
     */
    fun getMovieCertification(movieId: Long): CertificationResponse {
        val cacheKey = CacheKeys.Movie.certification(movieId)

        // Cache'den kontrol et
        cacheService.get(cacheKey, CertificationResponse::class.java)?.let {
            logger.info("Movie certification cache HIT for movieId: {}", movieId)
            return it
        }

        // TMDB'den getir
        logger.info("Movie certification cache MISS for movieId: {}, fetching from TMDB", movieId)
        val tmdbResponse = tmdbApiClient.getReleaseDates(movieId)
        val response = mapToCertificationResponse(movieId, tmdbResponse)

        // Cache'e yaz (24 saat)
        cacheService.set(cacheKey, response, CacheKeys.TTL.LONG)

        return response
    }

    // ==================== Mapping Functions ====================

    private fun mapToPopularMoviesResponse(tmdb: TmdbPopularResponse, page: Int): PopularMoviesResponse {
        return PopularMoviesResponse(
            movies = tmdb.results.map { movie ->
                PopularMovieDto(
                    id = movie.id,
                    title = movie.title,
                    overview = movie.overview ?: "",
                    posterPath = tmdbApiClient.buildPosterUrl(movie.poster_path),
                    rating = movie.vote_average,
                    releaseDate = movie.release_date
                )
            },
            page = page,
            totalPages = tmdb.total_pages,
            totalResults = tmdb.total_results
        )
    }

    private fun mapToMovieDetailResponse(tmdb: TmdbMovieDetailResponse): MovieDetailResponse {
        return MovieDetailResponse(
            id = tmdb.id,
            title = tmdb.title,
            overview = tmdb.overview ?: "",
            posterPath = tmdbApiClient.buildPosterUrl(tmdb.poster_path),
            backdropPath = tmdbApiClient.buildBackdropUrl(tmdb.backdrop_path),
            tagline = tmdb.tagline,
            runtime = tmdb.runtime,
            releaseDate = tmdb.release_date,
            voteAverage = tmdb.vote_average,
            originalLanguage = tmdb.original_language,
            genres = tmdb.genres.map { GenreDto(it.id, it.name) }
        )
    }

    private fun mapToMovieCreditsResponse(movieId: Long, tmdb: TmdbCreditsResponse): MovieCreditsResponse {
        return MovieCreditsResponse(
            movieId = movieId,
            cast = tmdb.cast
                .sortedBy { it.order ?: Int.MAX_VALUE }
                .map { cast ->
                    CastMemberDto(
                        id = cast.id,
                        name = cast.name,
                        character = cast.character ?: "",
                        profilePath = tmdbApiClient.buildProfileUrl(cast.profile_path)
                    )
                }
        )
    }

    private fun mapToMovieRecommendationsResponse(movieId: Long, tmdb: TmdbRecommendationsResponse): MovieRecommendationsResponse {
        return MovieRecommendationsResponse(
            movieId = movieId,
            recommendations = tmdb.results.map { movie ->
                RecommendedMovieDto(
                    id = movie.id,
                    title = movie.title,
                    posterPath = tmdbApiClient.buildPosterUrl(movie.poster_path),
                    voteAverage = movie.vote_average
                )
            }
        )
    }

    private fun mapToCertificationResponse(movieId: Long, tmdb: TmdbReleaseDatesResponse): CertificationResponse {
        // Önce TR'yi ara
        val trDates = tmdb.results.find { it.iso_3166_1 == "TR" }?.release_dates
        val trCert = trDates?.firstOrNull { !it.certification.isNullOrBlank() }?.certification

        if (!trCert.isNullOrBlank()) {
            return CertificationResponse(movieId, trCert, "TR")
        }

        // TR yoksa US'i ara
        val usDates = tmdb.results.find { it.iso_3166_1 == "US" }?.release_dates
        val usCert = usDates?.firstOrNull { !it.certification.isNullOrBlank() }?.certification

        if (!usCert.isNullOrBlank()) {
            return CertificationResponse(movieId, usCert, "US")
        }

        // Hiçbiri yoksa
        return CertificationResponse(movieId, "NR", "NR")
    }
}
