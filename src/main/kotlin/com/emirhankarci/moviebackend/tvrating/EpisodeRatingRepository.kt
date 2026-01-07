package com.emirhankarci.moviebackend.tvrating

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EpisodeRatingRepository : JpaRepository<EpisodeRating, Long> {
    
    fun findByUserIdAndSeriesId(userId: Long, seriesId: Long): List<EpisodeRating>
    
    fun findByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
        userId: Long, seriesId: Long, seasonNumber: Int, episodeNumber: Int
    ): EpisodeRating?
    
    fun existsByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
        userId: Long, seriesId: Long, seasonNumber: Int, episodeNumber: Int
    ): Boolean
    
    fun deleteByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
        userId: Long, seriesId: Long, seasonNumber: Int, episodeNumber: Int
    ): Int
}
