package com.emirhankarci.moviebackend.usercollection

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserCollectionRepository : JpaRepository<UserCollection, Long> {
    fun findByUserId(userId: Long): List<UserCollection>
    fun findByIdAndUserId(id: Long, userId: Long): Optional<UserCollection>
    fun findByUserIdAndName(userId: Long, name: String): Optional<UserCollection>
    fun existsByUserIdAndName(userId: Long, name: String): Boolean
    fun existsByUserIdAndNameAndIdNot(userId: Long, name: String, id: Long): Boolean
}
