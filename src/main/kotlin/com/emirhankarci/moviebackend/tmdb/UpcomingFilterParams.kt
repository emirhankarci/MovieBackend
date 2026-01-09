package com.emirhankarci.moviebackend.tmdb

/**
 * TMDB Discover API için upcoming movies filtre parametreleri.
 * Bu parametreler API seviyesinde filtreleme için kullanılır.
 */
data class UpcomingFilterParams(
    /** Minimum oy sayısı (vote_count.gte) */
    val minVoteCount: Int,
    
    /** Minimum film süresi dakika cinsinden (with_runtime.gte) */
    val minRuntime: Int,
    
    /** Hariç tutulacak tür ID'leri (without_genres) */
    val excludedGenreIds: List<Int>
)
