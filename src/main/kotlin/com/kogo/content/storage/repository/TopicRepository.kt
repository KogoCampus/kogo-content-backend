package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.TopicEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface TopicRepository : MongoRepository<TopicEntity, String> {
    fun findByGroupName(groupName: String): TopicEntity?
    fun findByOwnerId(ownerId: String): List<TopicEntity>
}