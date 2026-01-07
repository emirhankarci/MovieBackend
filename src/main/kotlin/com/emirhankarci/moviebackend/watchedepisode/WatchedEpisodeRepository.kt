package com.emirhankarci.moviebackend.watchedepisode

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface WatchedEpisodeRepository : JpaRepository<WatchedEpisode, Long> {
    
    fun findByUserIdAndSeriesId(userId: Long, seriesId: Long): List<WatchedEpisode>
    
    fun findByUserIdAndSeriesIdAndSeasonNumber(userId: Long, seriesId: Long, seasonNumber: Int): List<WatchedEpisode>
    
    fun findByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
        userId: Long, seriesId: Long, seasonNumber: Int, episodeNumber: Int
    ): WatchedEpisode?
    
    fun existsByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
        userId: Long, seriesId: Long, seasonNumber: Int, episodeNumber: Int
    ): Boolean
    
    fun deleteByUserIdAndSeriesIdAndSeasonNumberAndEpisodeNumber(
        userId: Long, seriesId: Long, seasonNumber: Int, episodeNumber: Int
    ): Int
    
    fun deleteByUserIdAndSeriesIdAndSeasonNumber(userId: Long, seriesId: Long, seasonNumber: Int): Int
    
    @Query("SELECT COUNT(w) FROM WatchedEpisode w WHERE w.user.id = :userId AND w.seriesId = :seriesId")
    fun countByUserIdAndSeriesId(userId: Long, seriesId: Long): Int
}
