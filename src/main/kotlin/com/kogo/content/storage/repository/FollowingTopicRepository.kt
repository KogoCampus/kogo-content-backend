package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.FollowingTopic
import org.springframework.data.mongodb.repository.MongoRepository

interface FollowingTopicRepository: MongoRepository<FollowingTopic, String> {
    fun findByOwnerId(ownerId: String): List<FollowingTopic>

    fun existsByOwnerIdAndTopicId(ownerId: String, topicId: String): Boolean

    fun findByOwnerIdAndTopicId(ownerId: String, topicId: String): List<FollowingTopic>
}
