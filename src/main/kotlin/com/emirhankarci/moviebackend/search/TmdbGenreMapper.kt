package com.emirhankarci.moviebackend.search

object TmdbGenreMapper {
    
    // Alias map for TMDB genre names that differ from our enum names
    private val genreAliasMap = mapOf(
        "SCIENCE FICTION" to "SCIFI",
        "SCIENCEFICTION" to "SCIFI",
        "SCI-FI" to "SCIFI",
        "TV MOVIE" to "TV_MOVIE",
        "TVMOVIE" to "TV_MOVIE"
    )
    
    private val genreToIdMap = mapOf(
        "ACTION" to 28,
        "ADVENTURE" to 12,
        "ANIMATION" to 16,
        "COMEDY" to 35,
        "CRIME" to 80,
        "DOCUMENTARY" to 99,
        "DRAMA" to 18,
        "FAMILY" to 10751,
        "FANTASY" to 14,
        "HISTORY" to 36,
        "HORROR" to 27,
        "MUSIC" to 10402,
        "MYSTERY" to 9648,
        "ROMANCE" to 10749,
        "SCIFI" to 878,
        "THRILLER" to 53,
        "TV_MOVIE" to 10770,
        "WAR" to 10752,
        "WESTERN" to 37
    )
    
    private val idToGenreMap = mapOf(
        28 to "Aksiyon",
        12 to "Macera",
        16 to "Animasyon",
        35 to "Komedi",
        80 to "Suç",
        99 to "Belgesel",
        18 to "Drama",
        10751 to "Aile",
        14 to "Fantastik",
        36 to "Tarih",
        27 to "Korku",
        10402 to "Müzik",
        9648 to "Gizem",
        10749 to "Romantik",
        878 to "Bilim Kurgu",
        53 to "Gerilim",
        10770 to "TV Film",
        10752 to "Savaş",
        37 to "Western"
    )
    
    private fun normalizeGenre(genre: String): String {
        val upper = genre.uppercase().trim()
        return genreAliasMap[upper] ?: upper
    }
    
    fun getGenreId(genre: String): Int? {
        return genreToIdMap[normalizeGenre(genre)]
    }
    
    fun getGenreName(genreId: Int): String {
        return idToGenreMap[genreId] ?: "Bilinmeyen"
    }
    
    fun isValidGenre(genre: String): Boolean {
        return genreToIdMap.containsKey(normalizeGenre(genre))
    }
    
    fun getAllGenres(): List<String> {
        return genreToIdMap.keys.toList()
    }
}
