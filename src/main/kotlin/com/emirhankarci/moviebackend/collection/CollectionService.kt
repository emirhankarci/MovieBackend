package com.emirhankarci.moviebackend.collection

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate

@Service
class CollectionService(
    private val restTemplate: RestTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CollectionService::class.java)
        private const val TMDB_API_URL = "https://api.themoviedb.org/3"
        private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
    }

    private val apiKey: String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    /**
     * Get prerequisite movies for a given movie ID
     */
    fun getPrerequisiteMovies(movieId: Long): CollectionResult {
        return try {
            // 1. Fetch movie details to get collection info
            val movieDetails = fetchMovieDetails(movieId)
                ?: return CollectionResult.Error("MOVIE_NOT_FOUND", "Film bulunamadı")

            // 2. Check if movie belongs to a collection
            val collectionInfo = movieDetails.belongs_to_collection
                ?: return CollectionResult.NoCollection("Bu film bir seriye ait değil")

            // 3. Fetch collection details
            val collectionDetails = fetchCollectionDetails(collectionInfo.id)
                ?: return CollectionResult.Error("COLLECTION_NOT_FOUND", "Koleksiyon bulunamadı")

            val allMovies = collectionDetails.parts ?: emptyList()

            // 4. Find target movie in collection
            val targetMovie = allMovies.find { it.id == movieId }
                ?: return CollectionResult.Error("MOVIE_NOT_IN_COLLECTION", "Film koleksiyonda bulunamadı")

            // 5. Filter prerequisites (movies released before target)
            val prerequisites = filterPrerequisites(allMovies, targetMovie.release_date)

            // 6. Check if there are any prerequisites
            if (prerequisites.isEmpty()) {
                return CollectionResult.NoPrerequisites(
                    collectionId = collectionInfo.id,
                    collectionName = collectionInfo.name,
                    message = "Bu serinin ilk filmi, önce izlenmesi gereken film yok"
                )
            }

            // 7. Sort and transform to response
            val sortedPrerequisites = sortByReleaseDate(prerequisites)
            val response = transformToResponse(collectionDetails, sortedPrerequisites, targetMovie)

            CollectionResult.Success(response)

        } catch (e: ResourceAccessException) {
            logger.error("TMDB API unavailable: {}", e.message)
            CollectionResult.Error("EXTERNAL_SERVICE_ERROR", "Film servisi şu anda kullanılamıyor")
        } catch (e: HttpClientErrorException.NotFound) {
            logger.error("Movie not found in TMDB: {}", movieId)
            CollectionResult.Error("MOVIE_NOT_FOUND", "Film bulunamadı")
        } catch (e: HttpClientErrorException) {
            logger.error("TMDB API error: {}", e.message)
            CollectionResult.Error("EXTERNAL_SERVICE_ERROR", "Film servisi hatası")
        } catch (e: Exception) {
            logger.error("Failed to get prerequisites for movie {}: {}", movieId, e.message, e)
            CollectionResult.Error("INTERNAL_ERROR", "Beklenmeyen bir hata oluştu")
        }
    }

    /**
     * Fetch movie details from TMDB API
     */
    internal fun fetchMovieDetails(movieId: Long): TmdbMovieWithCollection? {
        val url = "$TMDB_API_URL/movie/$movieId?api_key=$apiKey&language=tr-TR"
        logger.debug("Fetching movie details: {}", url.replace(apiKey, "***"))

        return restTemplate.getForObject(url, TmdbMovieWithCollection::class.java)
    }

    /**
     * Fetch collection details from TMDB API
     */
    internal fun fetchCollectionDetails(collectionId: Long): TmdbCollectionResponse? {
        val url = "$TMDB_API_URL/collection/$collectionId?api_key=$apiKey&language=tr-TR"
        logger.debug("Fetching collection details: {}", url.replace(apiKey, "***"))

        return restTemplate.getForObject(url, TmdbCollectionResponse::class.java)
    }


    /**
     * Filter movies released before the target movie's release date
     */
    internal fun filterPrerequisites(
        movies: List<TmdbCollectionMovie>,
        targetReleaseDate: String?
    ): List<TmdbCollectionMovie> {
        if (targetReleaseDate.isNullOrBlank()) {
            logger.warn("Target movie has no release date, returning empty prerequisites")
            return emptyList()
        }

        return movies.filter { movie ->
            val movieDate = movie.release_date
            if (movieDate.isNullOrBlank()) {
                false
            } else {
                movieDate < targetReleaseDate
            }
        }
    }

    /**
     * Sort movies by release date in ascending order (oldest first)
     */
    internal fun sortByReleaseDate(movies: List<TmdbCollectionMovie>): List<TmdbCollectionMovie> {
        return movies.sortedBy { it.release_date ?: "" }
    }

    /**
     * Format poster path as full URL
     */
    internal fun formatPosterUrl(posterPath: String?): String? {
        return posterPath?.let { "$TMDB_IMAGE_BASE_URL$it" }
    }

    /**
     * Extract year from release date (YYYY-MM-DD format)
     */
    internal fun extractReleaseYear(releaseDate: String?): Int {
        return releaseDate?.take(4)?.toIntOrNull() ?: 0
    }

    /**
     * Build display message for prerequisites
     */
    internal fun buildDisplayMessage(targetMovieTitle: String): String {
        return "\"$targetMovieTitle\" filmini izlemeden önce bunları izle"
    }

    /**
     * Transform TMDB data to response format
     */
    internal fun transformToResponse(
        collection: TmdbCollectionResponse,
        prerequisites: List<TmdbCollectionMovie>,
        targetMovie: TmdbCollectionMovie
    ): PrerequisiteMoviesResponse {
        val prerequisitesDtos = prerequisites.map { movie ->
            CollectionMovieDto(
                id = movie.id,
                title = movie.title,
                posterPath = formatPosterUrl(movie.poster_path),
                releaseDate = movie.release_date,
                releaseYear = extractReleaseYear(movie.release_date)
            )
        }

        val targetMovieDto = CollectionMovieDto(
            id = targetMovie.id,
            title = targetMovie.title,
            posterPath = formatPosterUrl(targetMovie.poster_path),
            releaseDate = targetMovie.release_date,
            releaseYear = extractReleaseYear(targetMovie.release_date)
        )

        return PrerequisiteMoviesResponse(
            collectionId = collection.id,
            collectionName = collection.name,
            totalMoviesInCollection = collection.parts?.size ?: 0,
            targetMovie = targetMovieDto,
            displayMessage = buildDisplayMessage(targetMovie.title),
            prerequisites = prerequisitesDtos
        )
    }
}
