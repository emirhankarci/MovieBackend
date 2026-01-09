package com.emirhankarci.moviebackend.tvsuggestion

import com.emirhankarci.moviebackend.chat.AiResult
import com.emirhankarci.moviebackend.chat.AiService
import com.emirhankarci.moviebackend.featured.FeaturedTvSeries
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TvHookMessageGenerator(
    private val aiService: AiService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvHookMessageGenerator::class.java)
        private val objectMapper = jacksonObjectMapper()
        private const val MIN_MESSAGE_LENGTH = 10
        private const val MAX_MESSAGE_LENGTH = 100
    }

    /**
     * Generate personalized hook messages for featured TV series
     */
    fun generateHookMessages(
        tvSeries: List<FeaturedTvSeries>,
        userProfile: TvUserProfile?
    ): List<TvHookMessage> {
        val prompt = buildPrompt(tvSeries, userProfile)

        return when (val result = aiService.generateSuggestions(prompt)) {
            is AiResult.Success -> {
                val messages = parseResponse(result.data, tvSeries)
                logger.info(
                    "Generated {} TV hook messages for user {}",
                    messages.size, userProfile?.userId ?: "anonymous"
                )
                messages
            }
            is AiResult.Error -> {
                logger.error("AI error generating TV hook messages: {}", result.message)
                generateFallbackMessages(tvSeries)
            }
        }
    }

    /**
     * Build AI prompt from TV series and user profile
     */
    fun buildPrompt(tvSeries: List<FeaturedTvSeries>, userProfile: TvUserProfile?): String {
        val profileSection = buildProfileSection(userProfile)
        val seriesSection = buildSeriesSection(tvSeries)

        return """
Sen yaratıcı bir dizi öneri asistanısın. Kullanıcıya özel, kısa ve çekici hook mesajları üreteceksin.

$profileSection

$seriesSection

MESAJ STİLLERİ (her dizi için farklı bir stil kullan):
1. Kişisel bağlantı: "Senin için biçilmiş kaftan!" 
2. Merak uyandırıcı: "Sonu seni şaşırtacak..."
3. Sosyal kanıt: "Herkes bundan bahsediyor!"
4. Mood bazlı: "Akşam dizi keyfi için ideal"
5. Tür bazlı: "Gerilim dozunu al!"
6. Watchlist referansı: "Listendeki X'e bayıldıysan..."
7. Soru formatı: "Drama sever misin?"
8. Emoji ile: "🔥 Bu hafta kaçırılmaz!"
9. Kısa ve net: "Tam zamanı!"
10. Öneri tonu: "Bunu dene, pişman olmazsın"

KURALLAR:
- Her mesaj 15-60 karakter arası olsun (çok kısa veya çok uzun olmasın)
- Türkçe yaz
- Her mesaj BENZERSİZ olsun, aynı kalıpları tekrarlama
- Samimi ve arkadaşça bir ton kullan
- Dizi adını mesajda KULLANMA

JSON formatında yanıt ver (sadece JSON array, başka bir şey yazma):
[{"seriesId": 123, "message": "Gerilim dozunu al! 🎬"}]
        """.trimIndent()
    }

    private fun buildProfileSection(userProfile: TvUserProfile?): String {
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

        // Watched TV series (top 5)
        if (userProfile.watchedTvSeries.isNotEmpty()) {
            val topWatched = userProfile.watchedTvSeries.take(5).map { it.seriesName }
            sections.add("- Son izlediği diziler: ${topWatched.joinToString(", ")}")
        }

        // TV Watchlist (top 5)
        if (userProfile.tvWatchlist.isNotEmpty()) {
            val topWatchlist = userProfile.tvWatchlist.take(5).map { it.seriesName }
            sections.add("- Watchlist'indeki diziler: ${topWatchlist.joinToString(", ")}")
        }

        // Personalization tier info
        val tierInfo = when (userProfile.personalizationTier) {
            TvPersonalizationTier.FULL -> "Detaylı profil mevcut"
            TvPersonalizationTier.PREFERENCES_BASED -> "Sadece tercihler mevcut"
            TvPersonalizationTier.WATCHLIST_BASED -> "Sadece watchlist mevcut"
            TvPersonalizationTier.DIVERSE_POPULAR -> "Yeni kullanıcı"
        }
        sections.add("- Profil durumu: $tierInfo")

        return sections.joinToString("\n")
    }

    private fun buildSeriesSection(tvSeries: List<FeaturedTvSeries>): String {
        val seriesLines = tvSeries.mapIndexed { index, series ->
            "${index + 1}. ${series.name} (${series.firstAirYear}) - Türler: ${series.genres.joinToString(", ")} - Rating: ${series.rating}"
        }
        return "Diziler:\n${seriesLines.joinToString("\n")}"
    }

    /**
     * Parse AI response into TvHookMessage list
     */
    fun parseResponse(aiResponse: String, tvSeries: List<FeaturedTvSeries>): List<TvHookMessage> {
        return try {
            // Extract JSON from response (AI might add extra text)
            val jsonStart = aiResponse.indexOf('[')
            val jsonEnd = aiResponse.lastIndexOf(']')

            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
                logger.warn("Could not find JSON array in AI response")
                return generateFallbackMessages(tvSeries)
            }

            val jsonString = aiResponse.substring(jsonStart, jsonEnd + 1)
            val parsed: List<Map<String, Any>> = objectMapper.readValue(jsonString)

            val messages = parsed.mapNotNull { item ->
                val seriesId = when (val id = item["seriesId"]) {
                    is Int -> id.toLong()
                    is Long -> id
                    is Number -> id.toLong()
                    else -> null
                }
                val message = item["message"] as? String

                if (seriesId != null && message != null) {
                    val trimmedMessage = trimMessage(message)
                    TvHookMessage(seriesId, trimmedMessage)
                } else {
                    null
                }
            }

            // Ensure we have messages for all TV series
            val messageMap = messages.associateBy { it.seriesId }
            tvSeries.map { series ->
                messageMap[series.id] ?: TvHookMessage(series.id, generateGenericMessage(series))
            }
        } catch (e: Exception) {
            logger.error("Failed to parse AI response: {}", e.message)
            generateFallbackMessages(tvSeries)
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
    private fun generateFallbackMessages(tvSeries: List<FeaturedTvSeries>): List<TvHookMessage> {
        return tvSeries.map { series ->
            TvHookMessage(series.id, generateGenericMessage(series))
        }
    }

    /**
     * Generate a generic message based on TV series properties
     */
    private fun generateGenericMessage(series: FeaturedTvSeries): String {
        val genre = series.genres.firstOrNull() ?: "Dizi"
        return when {
            series.rating >= 8.0 -> "Bu hafta en çok beğenilen $genre dizisi!"
            series.rating >= 7.0 -> "Popüler $genre dizisi, kaçırma!"
            else -> "Bu hafta trend olan $genre dizisi"
        }
    }
}

/**
 * Hook message for a specific TV series
 */
data class TvHookMessage(
    val seriesId: Long,
    val message: String
)
