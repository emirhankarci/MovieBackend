package com.emirhankarci.moviebackend.tvcollection

import com.emirhankarci.moviebackend.common.PageResponse
import com.emirhankarci.moviebackend.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TvCollectionService(
    private val tvCollectionRepository: TvCollectionRepository,
    private val tvCollectionSeriesRepository: TvCollectionSeriesRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TvCollectionService::class.java)
    }

    @Transactional
    fun createCollection(user: User, request: CreateTvCollectionRequest): TvCollectionResult<TvCollectionSummaryResponse> {
        val collection = TvCollection(
            user = user,
            name = request.name,
            description = request.description
        )
        val saved = tvCollectionRepository.save(collection)
        logger.info("Created TV collection '{}' for user {}", request.name, user.id)
        
        return TvCollectionResult.Success(saved.toSummaryResponse(emptyList(), 0))
    }

    @Transactional
    fun updateCollection(user: User, collectionId: Long, request: UpdateTvCollectionRequest): TvCollectionResult<TvCollectionSummaryResponse> {
        val collection = tvCollectionRepository.findByIdAndUserId(collectionId, user.id!!)
            ?: return TvCollectionResult.Error("COLLECTION_NOT_FOUND", "Koleksiyon bulunamadı")

        val updated = collection.copy(
            name = request.name ?: collection.name,
            description = request.description ?: collection.description,
            updatedAt = LocalDateTime.now()
        )
        val saved = tvCollectionRepository.save(updated)
        
        val coverPosters = getCoverPosters(collectionId)
        val seriesCount = tvCollectionSeriesRepository.countByCollectionId(collectionId)
        
        logger.info("Updated TV collection {} for user {}", collectionId, user.id)
        return TvCollectionResult.Success(saved.toSummaryResponse(coverPosters, seriesCount))
    }

    @Transactional
    fun deleteCollection(user: User, collectionId: Long): TvCollectionResult<Unit> {
        val deleted = tvCollectionRepository.deleteByIdAndUserId(collectionId, user.id!!)
        
        return if (deleted > 0) {
            logger.info("Deleted TV collection {} for user {}", collectionId, user.id)
            TvCollectionResult.Success(Unit)
        } else {
            TvCollectionResult.Error("COLLECTION_NOT_FOUND", "Koleksiyon bulunamadı")
        }
    }

    fun getCollections(user: User): List<TvCollectionSummaryResponse> {
        val collections = tvCollectionRepository.findByUserId(user.id!!)
        
        return collections.map { collection ->
            val collectionId = collection.id!!
            val coverPosters = getCoverPosters(collectionId)
            val seriesCount = tvCollectionSeriesRepository.countByCollectionId(collectionId)
            collection.toSummaryResponse(coverPosters, seriesCount)
        }
    }

    fun getCollectionDetail(user: User, collectionId: Long, page: Int, size: Int): TvCollectionResult<TvCollectionDetailResponse> {
        val collection = tvCollectionRepository.findByIdAndUserId(collectionId, user.id!!)
            ?: return TvCollectionResult.Error("COLLECTION_NOT_FOUND", "Koleksiyon bulunamadı")

        val pageable = PageRequest.of(page, size)
        val seriesPage = tvCollectionSeriesRepository.findByCollectionId(collectionId, pageable)
        
        val coverPosters = getCoverPosters(collectionId)
        val seriesCount = tvCollectionSeriesRepository.countByCollectionId(collectionId)
        val collId = collection.id!!

        val response = TvCollectionDetailResponse(
            id = collId,
            name = collection.name,
            description = collection.description,
            coverPosters = coverPosters,
            seriesCount = seriesCount,
            createdAt = collection.createdAt,
            series = PageResponse.from(seriesPage) { it.toDto() }
        )

        return TvCollectionResult.Success(response)
    }

    @Transactional
    fun addSeriesToCollection(user: User, collectionId: Long, request: AddSeriesToCollectionRequest): TvCollectionResult<TvCollectionSeriesDto> {
        val userId = user.id!!
        if (!tvCollectionRepository.existsByIdAndUserId(collectionId, userId)) {
            return TvCollectionResult.Error("COLLECTION_NOT_FOUND", "Koleksiyon bulunamadı")
        }

        if (tvCollectionSeriesRepository.existsByCollectionIdAndSeriesId(collectionId, request.seriesId)) {
            return TvCollectionResult.Error("SERIES_ALREADY_IN_COLLECTION", "Bu dizi zaten koleksiyonda")
        }

        val collection = tvCollectionRepository.findByIdAndUserId(collectionId, userId)!!
        val collectionSeries = TvCollectionSeries(
            collection = collection,
            seriesId = request.seriesId,
            seriesName = request.seriesName,
            posterPath = request.posterPath
        )
        
        val saved = tvCollectionSeriesRepository.save(collectionSeries)
        
        // Update collection's updatedAt
        tvCollectionRepository.save(collection.copy(updatedAt = LocalDateTime.now()))
        
        logger.info("Added series {} to collection {} for user {}", request.seriesId, collectionId, userId)
        return TvCollectionResult.Success(saved.toDto())
    }

    @Transactional
    fun removeSeriesFromCollection(user: User, collectionId: Long, seriesId: Long): TvCollectionResult<Unit> {
        val userId = user.id!!
        if (!tvCollectionRepository.existsByIdAndUserId(collectionId, userId)) {
            return TvCollectionResult.Error("COLLECTION_NOT_FOUND", "Koleksiyon bulunamadı")
        }

        val deleted = tvCollectionSeriesRepository.deleteByCollectionIdAndSeriesId(collectionId, seriesId)
        
        return if (deleted > 0) {
            // Update collection's updatedAt
            val collection = tvCollectionRepository.findByIdAndUserId(collectionId, userId)!!
            tvCollectionRepository.save(collection.copy(updatedAt = LocalDateTime.now()))
            
            logger.info("Removed series {} from collection {} for user {}", seriesId, collectionId, userId)
            TvCollectionResult.Success(Unit)
        } else {
            TvCollectionResult.Error("SERIES_NOT_IN_COLLECTION", "Bu dizi koleksiyonda değil")
        }
    }

    fun getSeriesCollections(user: User, seriesId: Long): SeriesCollectionStatusResponse {
        val collections = tvCollectionSeriesRepository.findCollectionsBySeriesIdAndUserId(seriesId, user.id!!)
        
        return SeriesCollectionStatusResponse(
            seriesId = seriesId,
            collections = collections.map { 
                val collId = it.id!!
                CollectionInfoDto(collId, it.name) 
            }
        )
    }

    private fun getCoverPosters(collectionId: Long): List<String?> {
        val pageable = PageRequest.of(0, 4)
        return tvCollectionSeriesRepository.findTop4ByCollectionIdOrderByAddedAtDesc(collectionId, pageable)
            .map { it.posterPath }
    }

    private fun TvCollection.toSummaryResponse(coverPosters: List<String?>, seriesCount: Int) = TvCollectionSummaryResponse(
        id = this.id!!,
        name = this.name,
        description = this.description,
        coverPosters = coverPosters,
        seriesCount = seriesCount,
        createdAt = this.createdAt
    )

    private fun TvCollectionSeries.toDto() = TvCollectionSeriesDto(
        id = this.id!!,
        seriesId = this.seriesId,
        seriesName = this.seriesName,
        posterPath = this.posterPath,
        addedAt = this.addedAt
    )
}
