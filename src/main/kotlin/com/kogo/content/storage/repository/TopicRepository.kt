package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.repository.traits.Followable
import org.springframework.data.mongodb.repository.MongoRepository

interface TopicRepository : MongoRepository<Topic, String>, Followable {
    fun findByTopicName(topicName: String): Topic?
    fun findAllByOwnerId(ownerId: String): List<Topic>

    fun existsByTopicName(topicName: String): Boolean
}
