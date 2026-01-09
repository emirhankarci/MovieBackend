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

        // Curated TV series for profile building
        val CURATED_TV_SERIES = listOf(
            CuratedTvSeries(1396, "Breaking Bad", "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg"),
            CuratedTvSeries(1399, "Game of Thrones", "/u3bZgnGQ9T01sWNhyveQz0wH0Hl.jpg"),
            CuratedTvSeries(60735, "The Flash", "/lJA2RCMfsWoskqlQhXPSLFQGXEJ.jpg"),
            CuratedTvSeries(66732, "Stranger Things", "/49WJfeN0moxb9IPfGn8AIqMGskD.jpg"),
            CuratedTvSeries(71446, "Money Heist", "/reEMJA1uzscCbkpeRJeTT2bjqUp.jpg"),
            CuratedTvSeries(94997, "House of the Dragon", "/z2yahl2uefxDCl0nogcRBstwruJ.jpg"),
            CuratedTvSeries(84958, "Loki", "/voHUmluYmKyleFkTu3lOXQG702u.jpg"),
            CuratedTvSeries(76479, "The Boys", "/stTEycfG9928HYGEISBFaG1ngjM.jpg"),
            CuratedTvSeries(63174, "Lucifer", "/4EYPN5mVIhKLfxGruy7Dy41dTVn.jpg"),
            CuratedTvSeries(1402, "The Walking Dead", "/xf9wuDcqlUPWABZNeDKPbZUjWx0.jpg"),
            CuratedTvSeries(60574, "Peaky Blinders", "/vUUqzWa2LnHvVHqbzWcxPBF5Bmi.jpg"),
            CuratedTvSeries(82856, "The Mandalorian", "/sWgBv7LV2PRoQgkxwlibdGXKz1S.jpg"),
            CuratedTvSeries(93405, "Squid Game", "/dDlEmu3EZ0Pgg93K2SVNLCjCSvE.jpg"),
            CuratedTvSeries(100088, "The Last of Us", "/uKvVjHNqB5VmOrdxqAt2F7J78ED.jpg"),
            CuratedTvSeries(1418, "The Big Bang Theory", "/ooBGRQBdbGzBxAVfExiO8r7kloA.jpg")
        )
    }

    @Transactional
    fun saveOrUpdatePreferences(username: String, request: SavePreferencesRequest): PreferencesResult<PreferencesResponse> {
        // Validate enums (stays in service layer - would need custom validators)
        when (val validation = request.validateEnums()) {
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
                favoriteTvSeriesIds = request.favoriteTvSeriesIds?.joinToString(","),
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
                favoriteTvSeriesIds = request.favoriteTvSeriesIds?.joinToString(","),
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

    fun getCuratedTvSeries(): List<CuratedTvSeries> {
        return CURATED_TV_SERIES
    }

    private fun UserPreferences.toResponse(): PreferencesResponse {
        return PreferencesResponse(
            genres = genres.split(",").filter { it.isNotBlank() },
            preferredEra = preferredEra,
            moods = moods.split(",").filter { it.isNotBlank() },
            favoriteMovieIds = favoriteMovieIds.split(",").filter { it.isNotBlank() }.map { it.toLong() },
            favoriteTvSeriesIds = favoriteTvSeriesIds?.split(",")?.filter { it.isNotBlank() }?.map { it.toLong() },
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
