package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Topic
import org.springframework.data.mongodb.repository.MongoRepository

interface TopicRepository : MongoRepository<Topic, String> {
    fun findByTopicName(topicName: String): Topic?
    fun findAllByOwnerId(ownerId: String): List<Topic>
}
