package com.emirhankarci.moviebackend.suggestion

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class SuggestionPromptBuilder {
    
    companion object {
        private val logger = LoggerFactory.getLogger(SuggestionPromptBuilder::class.java)
        private val HIGH_RATING_THRESHOLD = BigDecimal("7.0")
        private val LOW_RATING_THRESHOLD = BigDecimal("4.0")
    }

    /**
     * Main entry point - builds prompt based on user profile and tier.
     */
    fun buildPromptFromProfile(profile: UserProfile): String {
        logger.info("Building prompt for user {} with tier {}", profile.userId, profile.personalizationTier)
        
        return when (profile.personalizationTier) {
            PersonalizationTier.FULL -> buildFullPersonalizationPrompt(profile)
            PersonalizationTier.PREFERENCES_BASED -> buildPreferencesBasedPrompt(profile)
            PersonalizationTier.WATCHLIST_BASED -> buildWatchlistSimilarityPrompt(profile)
            PersonalizationTier.DIVERSE_POPULAR -> buildDiversePopularPrompt()
        }
    }

    /**
     * Full personalization - uses all available data.
     */
    private fun buildFullPersonalizationPrompt(profile: UserProfile): String {
        val highRatedMovies = profile.watchedMovies
            .filter { it.userRating != null && it.userRating >= HIGH_RATING_THRESHOLD }
            .map { "${it.title} (${it.userRating})" }
            .take(5)
            .joinToString(", ")
            .ifEmpty { "Yok" }
        
        val lowRatedMovies = profile.watchedMovies
            .filter { it.userRating != null && it.userRating <= LOW_RATING_THRESHOLD }
            .map { "${it.title} (${it.userRating})" }
            .take(3)
            .joinToString(", ")
            .ifEmpty { "Yok" }
        
        val prefs = profile.preferences
        val genres = prefs?.genres?.joinToString(", ") ?: "Belirtilmemiş"
        val era = prefs?.preferredEra ?: "Belirtilmemiş"
        val moods = prefs?.moods?.joinToString(", ") ?: "Belirtilmemiş"
        val favoriteIds = prefs?.favoriteMovieIds?.joinToString(", ") ?: "Yok"
        
        val watchlist = profile.watchlistMovies.map { it.title }.take(10).joinToString(", ").ifEmpty { "Yok" }
        val exclusionList = profile.exclusionList.allExcludedTitles.take(20).joinToString(", ")
        
        return """
Sen bir film öneri uzmanısın. Kullanıcının detaylı profili aşağıda.

KULLANICININ SEVDİĞİ FİLMLER (Yüksek puan verdiği):
$highRatedMovies

KULLANICININ SEVMEDİĞİ FİLMLER (Düşük puan verdiği):
$lowRatedMovies

TERCİH EDİLEN TÜRLER: $genres
TERCİH EDİLEN DÖNEM: $era
RUH HALİ TERCİHLERİ: $moods

FAVORİ FİLM ID'LERİ: $favoriteIds

İZLEME LİSTESİ: $watchlist

ÖNERİLMEMESİ GEREKEN FİLMLER (zaten izlemiş/listede):
$exclusionList

KURALLAR:
1. Yukarıdaki profile uygun 4 film öner
2. Sevdiği filmlere benzer, sevmediği filmlerden farklı öner
3. Tercih edilen türlere ve döneme uygun seç
4. Exclusion listesindeki filmleri KESİNLİKLE önerme
5. Her filmin İNGİLİZCE orijinal adını ver
6. Sadece TMDB'de en az 1000 oy almış filmler öner

ZORUNLU JSON FORMAT:
{
  "recommendations": [
    {"title": "Film Adı 1"},
    {"title": "Film Adı 2"},
    {"title": "Film Adı 3"},
    {"title": "Film Adı 4"}
  ]
}

Sadece JSON döndür, başka metin yazma.
        """.trimIndent()
    }


    /**
     * Preferences-based - for new users who completed onboarding but have no watch history.
     */
    private fun buildPreferencesBasedPrompt(profile: UserProfile): String {
        val prefs = profile.preferences!!
        val genres = prefs.genres.joinToString(", ")
        val era = prefs.preferredEra
        val moods = prefs.moods.joinToString(", ")
        val favoriteIds = prefs.favoriteMovieIds.joinToString(", ").ifEmpty { "Yok" }
        
        return """
Sen bir film öneri uzmanısın. Yeni bir kullanıcı için tercihlerine göre öneriler yapacaksın.

TERCİH EDİLEN TÜRLER: $genres
TERCİH EDİLEN DÖNEM: $era
RUH HALİ TERCİHLERİ: $moods
FAVORİ FİLM ID'LERİ: $favoriteIds

KURALLAR:
1. Tercih edilen türlere ve döneme uygun 4 film öner
2. Ruh haline uygun filmler seç
3. Favori filmlere benzer filmler öner (varsa)
4. Her filmin İNGİLİZCE orijinal adını ver
5. Sadece TMDB'de en az 1000 oy almış filmler öner

ZORUNLU JSON FORMAT:
{
  "recommendations": [
    {"title": "Film Adı 1"},
    {"title": "Film Adı 2"},
    {"title": "Film Adı 3"},
    {"title": "Film Adı 4"}
  ]
}

Sadece JSON döndür, başka metin yazma.
        """.trimIndent()
    }

    /**
     * Watchlist-based - uses only watchlist for similarity (legacy behavior enhanced).
     */
    private fun buildWatchlistSimilarityPrompt(profile: UserProfile): String {
        val watchlist = profile.watchlistMovies.map { it.title }.joinToString(", ")
        val exclusionList = profile.exclusionList.allExcludedTitles.joinToString(", ")
        
        return """
Sen bir film öneri uzmanısın. Kullanıcının izleme listesindeki filmlere dayanarak 
kişiselleştirilmiş öneriler yapacaksın.

KULLANICININ İZLEME LİSTESİ:
$watchlist

ÖNERİLMEMESİ GEREKEN FİLMLER:
$exclusionList

KURALLAR:
1. Yukarıdaki filmlere benzer ama listede OLMAYAN 4 film öner
2. Sadece HOLLYWOOD veya çok popüler uluslararası filmler öner
3. Sadece TMDB'de en az 1000 oy almış filmler öner
4. Her filmin İNGİLİZCE orijinal adını ver
5. Her seferinde FARKLI filmler öner, tekrar etme

ZORUNLU JSON FORMAT:
{
  "recommendations": [
    {"title": "Film Adı 1"},
    {"title": "Film Adı 2"},
    {"title": "Film Adı 3"},
    {"title": "Film Adı 4"}
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
Sen bir film öneri uzmanısın. Yeni bir kullanıcı için çeşitli ve kaliteli filmler önereceksin.

KURALLAR:
1. 4 FARKLI türden birer film öner (aksiyon, drama, bilim kurgu, gerilim)
2. Her biri IMDb 7.5+ puanlı olmalı
3. Farklı dönemlerden seç (1 klasik pre-2000, 3 modern post-2010)
4. Herkesin beğenebileceği evrensel filmler seç
5. Her filmin İNGİLİZCE orijinal adını ver
6. Sadece TMDB'de en az 1000 oy almış filmler öner

ZORUNLU JSON FORMAT:
{
  "recommendations": [
    {"title": "Film Adı 1"},
    {"title": "Film Adı 2"},
    {"title": "Film Adı 3"},
    {"title": "Film Adı 4"}
  ]
}

Sadece JSON döndür, başka metin yazma.
        """.trimIndent()
    }

    // Legacy methods kept for backward compatibility
    fun buildPersonalizedPrompt(watchlistMovies: List<String>): String {
        val movieList = watchlistMovies.joinToString(", ")
        return """
Sen bir film öneri uzmanısın. Kullanıcının izleme listesindeki filmlere dayanarak 
kişiselleştirilmiş öneriler yapacaksın.

KULLANICININ İZLEME LİSTESİ:
$movieList

KURALLAR:
1. Yukarıdaki filmlere benzer ama listede OLMAYAN 4 film öner
2. Sadece HOLLYWOOD veya çok popüler uluslararası filmler öner
3. Sadece TMDB'de en az 1000 oy almış filmler öner
4. Her filmin İNGİLİZCE orijinal adını ver
5. Her seferinde FARKLI filmler öner, tekrar etme

ZORUNLU JSON FORMAT:
{
  "recommendations": [
    {"title": "Film Adı 1"},
    {"title": "Film Adı 2"},
    {"title": "Film Adı 3"},
    {"title": "Film Adı 4"}
  ]
}

Sadece JSON döndür, başka metin yazma.
        """.trimIndent()
    }

    fun buildGenericPrompt(): String {
        return buildDiversePopularPrompt()
    }
}
