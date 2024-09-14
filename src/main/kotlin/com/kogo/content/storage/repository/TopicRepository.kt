package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.TopicEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface TopicRepository : MongoRepository<TopicEntity, String> {
    fun findByTopicName(topicName: String): TopicEntity?
    fun findByOwner(owner: String): List<TopicEntity>

    fun existsByTopicName(topicName: String): Boolean
}
