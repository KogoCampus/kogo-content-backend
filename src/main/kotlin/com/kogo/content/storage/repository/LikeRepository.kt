package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Like
import org.springframework.data.mongodb.repository.MongoRepository

interface LikeRepository : MongoRepository<Like, String> {
    fun findByUserIdAndParentId(userId: String, parentId: String): Like?
}
