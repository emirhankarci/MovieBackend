package com.emirhankarci.moviebackend.featured

import com.emirhankarci.moviebackend.chat.AiResult
import com.emirhankarci.moviebackend.chat.AiService
import com.emirhankarci.moviebackend.suggestion.PersonalizationTier
import com.emirhankarci.moviebackend.suggestion.UserProfile
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HookMessageGenerator(
    private val aiService: AiService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(HookMessageGenerator::class.java)
        private val objectMapper = jacksonObjectMapper()
        private const val MIN_MESSAGE_LENGTH = 10
        private const val MAX_MESSAGE_LENGTH = 100
    }

    /**
     * Generate personalized hook messages for featured movies
     */
    fun generateHookMessages(
        movies: List<FeaturedMovie>,
        userProfile: UserProfile?
    ): List<HookMessage> {
        val prompt = buildPrompt(movies, userProfile)
        
        return when (val result = aiService.generateSuggestions(prompt)) {
            is AiResult.Success -> {
                val messages = parseResponse(result.data, movies)
                logger.info("Generated {} hook messages for user {}", 
                    messages.size, userProfile?.userId ?: "anonymous")
                messages
            }
            is AiResult.Error -> {
                logger.error("AI error generating hook messages: {}", result.message)
                emptyList()
            }
        }
    }

    /**
     * Build AI prompt from movies and user profile
     */
    fun buildPrompt(movies: List<FeaturedMovie>, userProfile: UserProfile?): String {
        val profileSection = buildProfileSection(userProfile)
        val moviesSection = buildMoviesSection(movies)
        
        return """
Sen yaratıcı bir film öneri asistanısın. Kullanıcıya özel, kısa ve çekici hook mesajları üreteceksin.

$profileSection

$moviesSection

MESAJ STİLLERİ (her film için farklı bir stil kullan):
1. Kişisel bağlantı: "Senin için biçilmiş kaftan!" 
2. Merak uyandırıcı: "Sonu seni şaşırtacak..."
3. Sosyal kanıt: "Herkes bundan bahsediyor!"
4. Mood bazlı: "Akşam film keyfi için ideal"
5. Tür bazlı: "Aksiyon dozunu al!"
6. Watchlist referansı: "Listendeki X'e bayıldıysan..."
7. Soru formatı: "Gerilim sever misin?"
8. Emoji ile: "🔥 Bu hafta kaçırılmaz!"
9. Kısa ve net: "Tam zamanı!"
10. Öneri tonu: "Bunu dene, pişman olmazsın"

KURALLAR:
- Her mesaj 15-60 karakter arası olsun (çok kısa veya çok uzun olmasın)
- Türkçe yaz
- Her mesaj BENZERSİZ olsun, aynı kalıpları tekrarlama
- Samimi ve arkadaşça bir ton kullan
- Film adını mesajda KULLANMA

JSON formatında yanıt ver (sadece JSON array, başka bir şey yazma):
[{"movieId": 123, "message": "Aksiyon dozunu al! 🎬"}]
        """.trimIndent()
    }

    private fun buildProfileSection(userProfile: UserProfile?): String {
        if (userProfile == null) {
            return "Kullanıcı Profili: Yeni kullanıcı, genel öneriler yap."
        }

        val sections = mutableListOf<String>()
        sections.add("Kullanıcı Profili:")
        
        // Preferences
        userProfile.preferences?.let { prefs ->
            if (prefs.genres.isNotEmpty()) {
                sections.add("- Favori türler: ${prefs.genres.joinToString(", ")}")
            }
            if (prefs.moods.isNotEmpty()) {
                sections.add("- Mood tercihleri: ${prefs.moods.joinToString(", ")}")
            }
            if (prefs.preferredEra.isNotBlank()) {
                sections.add("- Tercih ettiği dönem: ${prefs.preferredEra}")
            }
        }
        
        // Watched movies (top 5)
        if (userProfile.watchedMovies.isNotEmpty()) {
            val topWatched = userProfile.watchedMovies.take(5).map { it.title }
            sections.add("- Son izlediği filmler: ${topWatched.joinToString(", ")}")
        }
        
        // Watchlist (top 5)
        if (userProfile.watchlistMovies.isNotEmpty()) {
            val topWatchlist = userProfile.watchlistMovies.take(5).map { it.title }
            sections.add("- Watchlist'indeki filmler: ${topWatchlist.joinToString(", ")}")
        }
        
        // Personalization tier info
        val tierInfo = when (userProfile.personalizationTier) {
            PersonalizationTier.FULL -> "Detaylı profil mevcut"
            PersonalizationTier.PREFERENCES_BASED -> "Sadece tercihler mevcut"
            PersonalizationTier.WATCHLIST_BASED -> "Sadece watchlist mevcut"
            PersonalizationTier.DIVERSE_POPULAR -> "Yeni kullanıcı"
        }
        sections.add("- Profil durumu: $tierInfo")
        
        return sections.joinToString("\n")
    }

    private fun buildMoviesSection(movies: List<FeaturedMovie>): String {
        val movieLines = movies.mapIndexed { index, movie ->
            "${index + 1}. ${movie.title} (${movie.releaseYear}) - Türler: ${movie.genres.joinToString(", ")} - Rating: ${movie.rating}"
        }
        return "Filmler:\n${movieLines.joinToString("\n")}"
    }

    /**
     * Parse AI response into HookMessage list
     */
    fun parseResponse(aiResponse: String, movies: List<FeaturedMovie>): List<HookMessage> {
        return try {
            // Extract JSON from response (AI might add extra text)
            val jsonStart = aiResponse.indexOf('[')
            val jsonEnd = aiResponse.lastIndexOf(']')
            
            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
                logger.warn("Could not find JSON array in AI response")
                return generateFallbackMessages(movies)
            }
            
            val jsonString = aiResponse.substring(jsonStart, jsonEnd + 1)
            val parsed: List<Map<String, Any>> = objectMapper.readValue(jsonString)
            
            val messages = parsed.mapNotNull { item ->
                val movieId = when (val id = item["movieId"]) {
                    is Int -> id.toLong()
                    is Long -> id
                    is Number -> id.toLong()
                    else -> null
                }
                val message = item["message"] as? String
                
                if (movieId != null && message != null) {
                    val trimmedMessage = trimMessage(message)
                    HookMessage(movieId, trimmedMessage)
                } else {
                    null
                }
            }
            
            // Ensure we have messages for all movies
            val messageMap = messages.associateBy { it.movieId }
            movies.map { movie ->
                messageMap[movie.id] ?: HookMessage(movie.id, generateGenericMessage(movie))
            }
        } catch (e: Exception) {
            logger.error("Failed to parse AI response: {}", e.message)
            generateFallbackMessages(movies)
        }
    }

    /**
     * Trim message to fit within bounds
     */
    private fun trimMessage(message: String): String {
        val trimmed = message.trim()
        return when {
            trimmed.length < MIN_MESSAGE_LENGTH -> trimmed.padEnd(MIN_MESSAGE_LENGTH, '.')
            trimmed.length > MAX_MESSAGE_LENGTH -> trimmed.take(MAX_MESSAGE_LENGTH - 3) + "..."
            else -> trimmed
        }
    }

    /**
     * Generate fallback messages when AI fails
     */
    private fun generateFallbackMessages(movies: List<FeaturedMovie>): List<HookMessage> {
        return movies.map { movie ->
            HookMessage(movie.id, generateGenericMessage(movie))
        }
    }

    /**
     * Generate a generic message based on movie properties
     */
    private fun generateGenericMessage(movie: FeaturedMovie): String {
        val genre = movie.genres.firstOrNull() ?: "Film"
        return when {
            movie.rating >= 8.0 -> "Bu hafta en çok beğenilen $genre filmi!"
            movie.rating >= 7.0 -> "Popüler $genre filmi, kaçırma!"
            else -> "Bu hafta trend olan $genre filmi"
        }
    }
}
