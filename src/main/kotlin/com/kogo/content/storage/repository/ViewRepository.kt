package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.View
import org.springframework.data.mongodb.repository.MongoRepository

interface ViewRepository : MongoRepository<View, String> {
    fun findByUserIdAndParentId(userId: String, parentId: String): View?
}
