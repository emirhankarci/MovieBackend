package com.emirhankarci.moviebackend.tvsuggestion

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TvSuggestionPromptBuilder {

    companion object {
        private val logger = LoggerFactory.getLogger(TvSuggestionPromptBuilder::class.java)
    }

    /**
     * Main entry point - builds prompt based on TV user profile and tier.
     */
    fun buildPromptFromProfile(profile: TvUserProfile): String {
        logger.info("Building TV prompt for user {} with tier {}", profile.userId, profile.personalizationTier)

        return when (profile.personalizationTier) {
            TvPersonalizationTier.FULL -> buildFullPersonalizationPrompt(profile)
            TvPersonalizationTier.PREFERENCES_BASED -> buildPreferencesBasedPrompt(profile)
            TvPersonalizationTier.WATCHLIST_BASED -> buildWatchlistBasedPrompt(profile)
            TvPersonalizationTier.DIVERSE_POPULAR -> buildDiversePopularPrompt()
        }
    }

    /**
     * Full personalization - uses all available TV data.
     */
    private fun buildFullPersonalizationPrompt(profile: TvUserProfile): String {
        val watchedSeries = profile.watchedTvSeries
            .sortedByDescending { it.episodeCount }
            .take(5)
            .joinToString(", ") { "${it.seriesName} (${it.episodeCount} bölüm)" }
            .ifEmpty { "Yok" }

        val prefs = profile.preferences
        val genres = prefs?.genres?.joinToString(", ") ?: "Belirtilmemiş"
        val era = prefs?.preferredEra ?: "Belirtilmemiş"
        val moods = prefs?.moods?.joinToString(", ") ?: "Belirtilmemiş"

        val watchlist = profile.tvWatchlist.map { it.seriesName }.take(10).joinToString(", ").ifEmpty { "Yok" }
        val exclusionList = profile.exclusionList.allExcludedTitles.take(20).joinToString(", ")

        return """
Sen bir dizi öneri uzmanısın. Kullanıcının detaylı TV profili aşağıda.

KULLANICININ İZLEDİĞİ DİZİLER (En çok bölüm izlediği):
$watchedSeries

TERCİH EDİLEN TÜRLER: $genres
TERCİH EDİLEN DÖNEM: $era
RUH HALİ TERCİHLERİ: $moods

İZLEME LİSTESİ: $watchlist

ÖNERİLMEMESİ GEREKEN DİZİLER (zaten izlemiş/listede):
$exclusionList

KURALLAR:
1. Yukarıdaki profile uygun 6 dizi öner
2. İzlediği dizilere benzer diziler öner
3. Tercih edilen türlere ve döneme uygun seç
4. Exclusion listesindeki dizileri KESİNLİKLE önerme
5. Her dizinin İNGİLİZCE orijinal adını ver
6. Sadece TMDB'de en az 200 oy almış diziler öner

ZORUNLU JSON FORMAT:
{
  "recommendations": [
    {"title": "Dizi Adı 1"},
    {"title": "Dizi Adı 2"},
    {"title": "Dizi Adı 3"},
    {"title": "Dizi Adı 4"},
    {"title": "Dizi Adı 5"},
    {"title": "Dizi Adı 6"}
  ]
}

Sadece JSON döndür, başka metin yazma.
        """.trimIndent()
    }

    /**
     * Preferences-based - for new users who completed onboarding but have no watch history.
     */
    private fun buildPreferencesBasedPrompt(profile: TvUserProfile): String {
        val prefs = profile.preferences!!
        val genres = prefs.genres.joinToString(", ")
        val era = prefs.preferredEra
        val moods = prefs.moods.joinToString(", ")

        return """
Sen bir dizi öneri uzmanısın. Yeni bir kullanıcı için tercihlerine göre öneriler yapacaksın.

TERCİH EDİLEN TÜRLER: $genres
TERCİH EDİLEN DÖNEM: $era
RUH HALİ TERCİHLERİ: $moods

KURALLAR:
1. Tercih edilen türlere ve döneme uygun 6 dizi öner
2. Ruh haline uygun diziler seç
3. Her dizinin İNGİLİZCE orijinal adını ver
4. Sadece TMDB'de en az 200 oy almış diziler öner

ZORUNLU JSON FORMAT:
{
  "recommendations": [
    {"title": "Dizi Adı 1"},
    {"title": "Dizi Adı 2"},
    {"title": "Dizi Adı 3"},
    {"title": "Dizi Adı 4"},
    {"title": "Dizi Adı 5"},
    {"title": "Dizi Adı 6"}
  ]
}

Sadece JSON döndür, başka metin yazma.
        """.trimIndent()
    }

    /**
     * Watchlist-based - uses only TV watchlist for similarity.
     */
    private fun buildWatchlistBasedPrompt(profile: TvUserProfile): String {
        val watchlist = profile.tvWatchlist.map { it.seriesName }.joinToString(", ")
        val exclusionList = profile.exclusionList.allExcludedTitles.joinToString(", ")

        return """
Sen bir dizi öneri uzmanısın. Kullanıcının izleme listesindeki dizilere dayanarak 
kişiselleştirilmiş öneriler yapacaksın.

KULLANICININ İZLEME LİSTESİ:
$watchlist

ÖNERİLMEMESİ GEREKEN DİZİLER:
$exclusionList

KURALLAR:
1. Yukarıdaki dizilere benzer ama listede OLMAYAN 6 dizi öner
2. Sadece popüler ve kaliteli diziler öner
3. Sadece TMDB'de en az 200 oy almış diziler öner
4. Her dizinin İNGİLİZCE orijinal adını ver
5. Her seferinde FARKLI diziler öner, tekrar etme

ZORUNLU JSON FORMAT:
{
  "recommendations": [
    {"title": "Dizi Adı 1"},
    {"title": "Dizi Adı 2"},
    {"title": "Dizi Adı 3"},
    {"title": "Dizi Adı 4"},
    {"title": "Dizi Adı 5"},
    {"title": "Dizi Adı 6"}
  ]
}

Sadece JSON döndür, başka metin yazma.
        """.trimIndent()
    }

    /**
     * Diverse popular - for users with no data at all.
     */
    private fun buildDiversePopularPrompt(): String {
        return """
Sen bir dizi öneri uzmanısın. Yeni bir kullanıcı için çeşitli ve kaliteli diziler önereceksin.

KURALLAR:
1. 6 FARKLI türden birer dizi öner (drama, aksiyon, bilim kurgu, gerilim, komedi, fantastik)
2. Her biri IMDb 7.5+ puanlı olmalı
3. Farklı dönemlerden seç (2 klasik pre-2015, 4 modern post-2018)
4. Herkesin beğenebileceği evrensel diziler seç
5. Her dizinin İNGİLİZCE orijinal adını ver
6. Sadece TMDB'de en az 200 oy almış diziler öner

ZORUNLU JSON FORMAT:
{
  "recommendations": [
    {"title": "Dizi Adı 1"},
    {"title": "Dizi Adı 2"},
    {"title": "Dizi Adı 3"},
    {"title": "Dizi Adı 4"},
    {"title": "Dizi Adı 5"},
    {"title": "Dizi Adı 6"}
  ]
}

Sadece JSON döndür, başka metin yazma.
        """.trimIndent()
    }
}
