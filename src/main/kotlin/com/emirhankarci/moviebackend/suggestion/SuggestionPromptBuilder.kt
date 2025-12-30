package com.emirhankarci.moviebackend.suggestion

import org.springframework.stereotype.Component

@Component
class SuggestionPromptBuilder {

    fun buildPersonalizedPrompt(watchlistMovies: List<String>): String {
        val movieList = watchlistMovies.joinToString(", ")
        return """
Sen bir film öneri uzmanısın. Kullanıcının izleme listesindeki filmlere dayanarak 
kişiselleştirilmiş öneriler yapacaksın.

KULLANICININ İZLEME LİSTESİ:
$movieList

KURALLAR:
1. Yukarıdaki filmlere benzer ama listede OLMAYAN 4 film öner
2. Sadece IMDb puanı 7.0 üzeri filmler öner
3. Sadece 20.000'den fazla oy almış filmler öner
4. Her filmin İNGİLİZCE orijinal adını ver

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
        return """
Sen bir film öneri uzmanısın. Yeni bir kullanıcı için popüler ve kaliteli filmler önereceksin.

KURALLAR:
1. IMDb puanı 7.5 üzeri, çok beğenilen 4 film öner
2. Farklı türlerden seç (aksiyon, drama, bilim kurgu, gerilim)
3. Her filmin İNGİLİZCE orijinal adını ver

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
}
