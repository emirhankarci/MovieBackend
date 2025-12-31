package com.emirhankarci.moviebackend.search

object TmdbGenreMapper {
    
    private val genreToIdMap = mapOf(
        "ACTION" to 28,
        "SCIFI" to 878,
        "HORROR" to 27,
        "DRAMA" to 18,
        "ANIMATION" to 16,
        "COMEDY" to 35,
        "ROMANCE" to 10749,
        "THRILLER" to 53,
        "FANTASY" to 14,
        "DOCUMENTARY" to 99
    )
    
    private val idToGenreMap = mapOf(
        28 to "Aksiyon",
        878 to "Bilim Kurgu",
        27 to "Korku",
        18 to "Drama",
        16 to "Animasyon",
        35 to "Komedi",
        10749 to "Romantik",
        53 to "Gerilim",
        14 to "Fantastik",
        99 to "Belgesel",
        // Additional TMDB genres
        12 to "Macera",
        80 to "Suç",
        10751 to "Aile",
        36 to "Tarih",
        10402 to "Müzik",
        9648 to "Gizem",
        10752 to "Savaş",
        37 to "Western",
        10770 to "TV Film"
    )
    
    fun getGenreId(genre: String): Int? {
        return genreToIdMap[genre.uppercase()]
    }
    
    fun getGenreName(genreId: Int): String {
        return idToGenreMap[genreId] ?: "Bilinmeyen"
    }
    
    fun isValidGenre(genre: String): Boolean {
        return genreToIdMap.containsKey(genre.uppercase())
    }
    
    fun getAllGenres(): List<String> {
        return genreToIdMap.keys.toList()
    }
}
