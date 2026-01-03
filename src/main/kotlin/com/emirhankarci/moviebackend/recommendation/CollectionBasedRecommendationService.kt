package com.emirhankarci.moviebackend.recommendation

import com.emirhankarci.moviebackend.search.TmdbGenreMapper
import com.emirhankarci.moviebackend.user.UserRepository
import com.emirhankarci.moviebackend.usercollection.UserCollectionMovieRepository
import com.emirhankarci.moviebackend.usercollection.UserCollectionRepository
import com.emirhankarci.moviebackend.watched.WatchedMovieRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import kotlin.random.Random

@Service
class CollectionBasedRecommendationService(
    private val userCollectionRepository: UserCollectionRepository,
    private val userCollectionMovieRepository: UserCollectionMovieRepository,
    private val watchedMovieRepository: WatchedMovieRepository,
    private val userRepository: UserRepository,
    private val restTemplate: RestTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CollectionBasedRecommendationService::class.java)
        private const val TMDB_API_URL = "https://api.themoviedb.org/3"
        private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
        private const val MOVIES_TO_SAMPLE = 3
        private const val TOP_RECOMMENDATIONS = 5
        private const val MIN_RATING = 6.0
    }

    private val apiKey: String = System.getenv("TMDB_API_KEY")
        ?: throw IllegalStateException("TMDB_API_KEY environment variable must be set!")

    fun getCollectionBasedRecommendations(username: String): CollectionRecommendationResult {
        val user = userRepository.findByUsername(username)
            ?: return CollectionRecommendationResult.Error("USER_NOT_FOUND", "Kullanıcı bulunamadı")

        val userId = user.id!!
        val collections = userCollectionRepository.findByUserId(userId)

        if (collections.isEmpty()) {
            return CollectionRecommendationResult.Empty("Henüz koleksiyonunuz yok")
        }

        // Filter collections that have at least 1 movie
        val nonEmptyCollections = collections.filter { collection ->
            userCollectionMovieRepository.countByCollectionId(collection.id!!) > 0
        }

        if (nonEmptyCollections.isEmpty()) {
            return CollectionRecommendationResult.Empty("Koleksiyonlarınızda henüz film yok")
        }

        return try {
            // Select random collection
            val selectedCollection = nonEmptyCollections[Random.nextInt(nonEmptyCollections.size)]
            val collectionId = selectedCollection.id!!
            logger.info("Selected collection '{}' for user {}", selectedCollection.name, username)

            // Get movies from collection
            val collectionMovies = userCollectionMovieRepository.findByCollectionIdOrderByAddedAtAsc(collectionId)
            
            // Sample 2-3 movies randomly
            val sampleSize = minOf(MOVIES_TO_SAMPLE, collectionMovies.size)
            val sampledMovies = collectionMovies.shuffled().take(sampleSize)
            
            logger.info("Sampled {} movies from collection", sampleSize)

            // Fetch similar movies for each sampled movie
            val allSimilarMovies = mutableListOf<TmdbRecommendedMovie>()
            for (movie in sampledMovies) {
                val similar = fetchSimilarMovies(movie.movieId)
                allSimilarMovies.addAll(similar)
            }

            // Get movies to exclude (watched + all collection movies)
            val watchedMovieIds = watchedMovieRepository.findByUserId(userId).map { it.movieId }.toSet()
            val allCollectionMovieIds = getAllUserCollectionMovieIds(userId)
            val excludeIds = watchedMovieIds + allCollectionMovieIds

            // Count frequency and filter
            val movieFrequency = allSimilarMovies
                .filter { it.id !in excludeIds }
                .filter { it.vote_average >= MIN_RATING }
                .groupingBy { it.id }
                .eachCount()

            // Get top movies by frequency
            val topMovieIds = movieFrequency.entries
                .sortedByDescending { it.value }
                .take(TOP_RECOMMENDATIONS)
                .map { it.key }

            // Get movie details for top recommendations
            val topMovies = allSimilarMovies
                .filter { it.id in topMovieIds }
                .distinctBy { it.id }
                .sortedByDescending { movieFrequency[it.id] ?: 0 }
                .take(TOP_RECOMMENDATIONS)

            val recommendations = topMovies.map { movie ->
                RecommendedMovie(
                    id = movie.id,
                    title = movie.title,
                    posterPath = movie.poster_path?.let { "$TMDB_IMAGE_BASE_URL$it" },
                    rating = movie.vote_average,
                    releaseYear = movie.release_date?.take(4)?.toIntOrNull() ?: 0,
                    genres = movie.genre_ids?.map { TmdbGenreMapper.getGenreName(it) } ?: emptyList()
                )
            }

            if (recommendations.isEmpty()) {
                return CollectionRecommendationResult.Empty("Bu koleksiyon için öneri bulunamadı")
            }

            val response = CollectionRecommendationsResponse(
                collectionId = collectionId,
                collectionName = selectedCollection.name,
                displayMessage = "'${selectedCollection.name}' koleksiyonundaki filmlerle benzer",
                recommendations = recommendations
            )

            logger.info("Returning {} collection-based recommendations for user {}", 
                recommendations.size, username)
            
            CollectionRecommendationResult.Success(response)

        } catch (e: ResourceAccessException) {
            logger.error("TMDB API unavailable: {}", e.message)
            CollectionRecommendationResult.Error("EXTERNAL_SERVICE_ERROR", "Film servisi şu anda kullanılamıyor")
        } catch (e: HttpClientErrorException) {
            logger.error("TMDB API error: {}", e.message)
            CollectionRecommendationResult.Error("EXTERNAL_SERVICE_ERROR", "Film servisi hatası")
        } catch (e: Exception) {
            logger.error("Failed to get collection recommendations for user {}: {}", username, e.message, e)
            CollectionRecommendationResult.Error("INTERNAL_ERROR", "Beklenmeyen bir hata oluştu")
        }
    }

    private fun fetchSimilarMovies(movieId: Long): List<TmdbRecommendedMovie> {
        val url = "$TMDB_API_URL/movie/$movieId/similar?api_key=$apiKey&language=tr-TR"
        logger.debug("Fetching similar movies for movie {}", movieId)
        
        return try {
            val response = restTemplate.getForObject(url, TmdbRecommendationsResponse::class.java)
            response?.results ?: emptyList()
        } catch (e: Exception) {
            logger.warn("Failed to fetch similar movies for {}: {}", movieId, e.message)
            emptyList()
        }
    }

    private fun getAllUserCollectionMovieIds(userId: Long): Set<Long> {
        val collections = userCollectionRepository.findByUserId(userId)
        return collections.flatMap { collection ->
            userCollectionMovieRepository.findByCollectionIdOrderByAddedAtAsc(collection.id!!)
                .map { it.movieId }
        }.toSet()
    }
}
