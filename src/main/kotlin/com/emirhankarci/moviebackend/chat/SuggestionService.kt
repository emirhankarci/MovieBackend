package com.emirhankarci.moviebackend.chat

import com.emirhankarci.moviebackend.preferences.Genre
import com.emirhankarci.moviebackend.preferences.UserPreferencesRepository
import com.emirhankarci.moviebackend.user.UserRepository
import com.emirhankarci.moviebackend.watched.WatchedMovieRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Month

@Service
class SuggestionService(
    private val userRepository: UserRepository,
    private val watchedMovieRepository: WatchedMovieRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SuggestionService::class.java)
        private const val MAX_SUGGESTIONS = 6

        // Genre emoji mappings
        private val GENRE_EMOJIS = mapOf(
            Genre.ACTION to "🔥",
            Genre.SCIFI to "🚀",
            Genre.HORROR to "👻",
            Genre.DRAMA to "🎭",
            Genre.ANIMATION to "🎨",
            Genre.COMEDY to "😂",
            Genre.ROMANCE to "💕",
            Genre.THRILLER to "😱",
            Genre.FANTASY to "🧙",
            Genre.DOCUMENTARY to "📚"
        )

        // Genre Turkish names
        private val GENRE_NAMES_TR = mapOf(
            Genre.ACTION to "Aksiyon",
            Genre.SCIFI to "Bilim Kurgu",
            Genre.HORROR to "Korku",
            Genre.DRAMA to "Dram",
            Genre.ANIMATION to "Animasyon",
            Genre.COMEDY to "Komedi",
            Genre.ROMANCE to "Romantik",
            Genre.THRILLER to "Gerilim",
            Genre.FANTASY to "Fantastik",
            Genre.DOCUMENTARY to "Belgesel"
        )
    }

    fun getSuggestions(username: String): SuggestionResult<SuggestionsResponse> {
        val user = userRepository.findByUsername(username)
            ?: return SuggestionResult.Error("User not found", SuggestionErrorCode.USER_NOT_FOUND)

        val userId = user.id ?: return SuggestionResult.Error("User ID not found", SuggestionErrorCode.USER_NOT_FOUND)

        val suggestions = mutableListOf<SuggestionChip>()

        // 1. Similar suggestions based on recently watched movies
        val similarSuggestions = generateSimilarSuggestions(userId)
        suggestions.addAll(similarSuggestions)

        // 2. Genre-based suggestions from user preferences
        val genreSuggestions = generateGenreSuggestions(userId)
        suggestions.addAll(genreSuggestions)

        // 3. Seasonal suggestions
        val seasonalSuggestions = generateSeasonalSuggestions()
        suggestions.addAll(seasonalSuggestions)

        // 4. Trending suggestions (always include at least one)
        val trendingSuggestions = generateTrendingSuggestions()
        suggestions.addAll(trendingSuggestions)

        // If no personalized suggestions, use defaults
        if (suggestions.isEmpty()) {
            suggestions.addAll(getDefaultSuggestions())
        }

        // Limit to MAX_SUGGESTIONS and remove duplicates
        val finalSuggestions = suggestions
            .distinctBy { it.text }
            .take(MAX_SUGGESTIONS)

        logger.debug("Generated {} suggestions for user {}", finalSuggestions.size, username)
        return SuggestionResult.Success(SuggestionsResponse(finalSuggestions))
    }


    private fun generateSimilarSuggestions(userId: Long): List<SuggestionChip> {
        val recentMovies = watchedMovieRepository.findByUserIdOrderByWatchedAtDesc(userId)
            .take(5)

        if (recentMovies.isEmpty()) return emptyList()

        return recentMovies.take(2).map { movie ->
            SuggestionChip(
                text = "${movie.movieTitle} gibi filmler",
                emoji = "🧠",
                type = SuggestionType.SIMILAR
            )
        }
    }

    private fun generateGenreSuggestions(userId: Long): List<SuggestionChip> {
        val preferences = userPreferencesRepository.findByUserId(userId).orElse(null)
            ?: return emptyList()

        val genres = preferences.genres.split(",")
            .mapNotNull { genreStr ->
                try {
                    Genre.valueOf(genreStr.trim())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            .take(2)

        return genres.map { genre ->
            val emoji = GENRE_EMOJIS[genre] ?: "🎬"
            val genreName = GENRE_NAMES_TR[genre] ?: genre.name
            SuggestionChip(
                text = "$genreName film öner",
                emoji = emoji,
                type = SuggestionType.GENRE
            )
        }
    }

    private fun generateSeasonalSuggestions(): List<SuggestionChip> {
        val currentMonth = LocalDate.now().month
        
        return when (currentMonth) {
            Month.DECEMBER -> listOf(
                SuggestionChip(
                    text = "Yılbaşı filmleri öner",
                    emoji = "🎄",
                    type = SuggestionType.SEASONAL
                )
            )
            Month.OCTOBER -> listOf(
                SuggestionChip(
                    text = "Cadılar Bayramı filmleri",
                    emoji = "🎃",
                    type = SuggestionType.SEASONAL
                )
            )
            Month.FEBRUARY -> listOf(
                SuggestionChip(
                    text = "Romantik film öner",
                    emoji = "💝",
                    type = SuggestionType.SEASONAL
                )
            )
            Month.JUNE, Month.JULY, Month.AUGUST -> listOf(
                SuggestionChip(
                    text = "Yaz filmleri öner",
                    emoji = "☀️",
                    type = SuggestionType.SEASONAL
                )
            )
            else -> emptyList()
        }
    }

    private fun generateTrendingSuggestions(): List<SuggestionChip> {
        return listOf(
            SuggestionChip(
                text = "Bu hafta ne izlesem?",
                emoji = "📅",
                type = SuggestionType.TRENDING
            )
        )
    }

    private fun getDefaultSuggestions(): List<SuggestionChip> {
        return listOf(
            SuggestionChip(
                text = "Bu hafta ne izlesem?",
                emoji = "📅",
                type = SuggestionType.TRENDING
            ),
            SuggestionChip(
                text = "Aksiyon film öner",
                emoji = "🔥",
                type = SuggestionType.GENRE
            ),
            SuggestionChip(
                text = "Komedi film öner",
                emoji = "😂",
                type = SuggestionType.GENRE
            ),
            SuggestionChip(
                text = "Bilim kurgu öner",
                emoji = "🚀",
                type = SuggestionType.GENRE
            ),
            SuggestionChip(
                text = "Korku filmi öner",
                emoji = "👻",
                type = SuggestionType.GENRE
            ),
            SuggestionChip(
                text = "Popüler filmler",
                emoji = "⭐",
                type = SuggestionType.TRENDING
            )
        )
    }
}
