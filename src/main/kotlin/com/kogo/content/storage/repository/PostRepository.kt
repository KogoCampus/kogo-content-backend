package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.PostEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface PostRepository: MongoRepository<PostEntity, String> {
    fun findByGroupId(groupId: String): List<PostEntity>
}
