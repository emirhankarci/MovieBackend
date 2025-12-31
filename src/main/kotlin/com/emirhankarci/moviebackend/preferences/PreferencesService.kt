package com.emirhankarci.moviebackend.preferences

import com.emirhankarci.moviebackend.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PreferencesService(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userRepository: UserRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PreferencesService::class.java)

        // Curated films for Holy Trinity selection
        val CURATED_FILMS = listOf(
            CuratedFilm(157336, "Interstellar", "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"),
            CuratedFilm(155, "The Dark Knight", "/qJ2tW6WMUDux911r6m7haRef0WH.jpg"),
            CuratedFilm(680, "Pulp Fiction", "/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg"),
            CuratedFilm(862, "Toy Story", "/uXDfjJbdP4ijW5hWSBrPrlKpxab.jpg"),
            CuratedFilm(138843, "The Conjuring", "/wVYREutTvI2tmxr6ujrHT704wGF.jpg"),
            CuratedFilm(11036, "The Notebook", "/rNzQyW4f8B8cQeg7Dgj3n6eT5k9.jpg"),
            CuratedFilm(238, "The Godfather", "/3bhkrj58Vtu7enYsRolD1fZdja1.jpg"),
            CuratedFilm(27205, "Inception", "/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg"),
            CuratedFilm(299536, "Avengers: Infinity War", "/7WsyChQLEftFiDOVTGkv3hFpyyt.jpg"),
            CuratedFilm(496243, "Parasite", "/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg"),
            CuratedFilm(129, "Spirited Away", "/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg"),
            CuratedFilm(98, "Gladiator", "/ty8TGRuvJLPUmAR1H1nRIsgwvim.jpg"),
            CuratedFilm(475557, "Joker", "/udDclJoHjfjb8Ekgsd4FDteOkCU.jpg"),
            CuratedFilm(324857, "Spider-Man: Into the Spider-Verse", "/iiZZdoQBEYBv6id8su7ImL0oCbD.jpg"),
            CuratedFilm(278, "The Shawshank Redemption", "/q6y0Go1tsGEsmtFryDOJo3dEmqu.jpg")
        )
    }

    @Transactional
    fun saveOrUpdatePreferences(username: String, request: SavePreferencesRequest): PreferencesResult<PreferencesResponse> {
        // Validate request
        when (val validation = request.validate()) {
            is PreferencesValidationResult.Invalid -> {
                logger.warn("Validation failed for user {}: {}", username, validation.message)
                return PreferencesResult.Error(validation.message)
            }
            is PreferencesValidationResult.Valid -> { /* continue */ }
        }

        val user = userRepository.findByUsername(username)
            ?: return PreferencesResult.Error("User not found!")

        val existing = userPreferencesRepository.findByUserId(user.id!!)
        val now = LocalDateTime.now()

        val preferences = if (existing.isPresent) {
            // Update existing preferences
            val updated = existing.get().copy(
                genres = request.genres.joinToString(",") { it.uppercase() },
                preferredEra = request.preferredEra.uppercase(),
                moods = request.moods.joinToString(",") { it.uppercase() },
                favoriteMovieIds = request.favoriteMovieIds.joinToString(","),
                updatedAt = now
            )
            userPreferencesRepository.save(updated)
            logger.info("Preferences updated for user {}", username)
            updated
        } else {
            // Create new preferences
            val newPreferences = UserPreferences(
                user = user,
                genres = request.genres.joinToString(",") { it.uppercase() },
                preferredEra = request.preferredEra.uppercase(),
                moods = request.moods.joinToString(",") { it.uppercase() },
                favoriteMovieIds = request.favoriteMovieIds.joinToString(","),
                createdAt = now,
                updatedAt = now
            )
            userPreferencesRepository.save(newPreferences)
            logger.info("Preferences created for user {}", username)
            newPreferences
        }

        return PreferencesResult.Success(preferences.toResponse())
    }

    fun getUserPreferences(username: String): PreferencesResult<PreferencesResponse?> {
        val user = userRepository.findByUsername(username)
            ?: return PreferencesResult.Error("User not found!")

        val preferences = userPreferencesRepository.findByUserId(user.id!!)

        return if (preferences.isPresent) {
            logger.debug("Returning preferences for user {}", username)
            PreferencesResult.Success(preferences.get().toResponse())
        } else {
            logger.debug("No preferences found for user {}", username)
            PreferencesResult.Success(null)
        }
    }

    fun hasPreferences(username: String): PreferencesResult<PreferencesStatusResponse> {
        val user = userRepository.findByUsername(username)
            ?: return PreferencesResult.Error("User not found!")

        val exists = userPreferencesRepository.existsByUserId(user.id!!)
        logger.debug("Preferences status for user {}: {}", username, exists)

        return PreferencesResult.Success(PreferencesStatusResponse(exists))
    }

    fun getCuratedFilms(): List<CuratedFilm> {
        return CURATED_FILMS
    }

    private fun UserPreferences.toResponse(): PreferencesResponse {
        return PreferencesResponse(
            genres = genres.split(",").filter { it.isNotBlank() },
            preferredEra = preferredEra,
            moods = moods.split(",").filter { it.isNotBlank() },
            favoriteMovieIds = favoriteMovieIds.split(",").filter { it.isNotBlank() }.map { it.toLong() },
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
