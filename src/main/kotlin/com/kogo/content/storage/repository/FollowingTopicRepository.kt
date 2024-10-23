package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.FollowingTopic
import org.springframework.data.mongodb.repository.MongoRepository

interface FollowingTopicRepository: MongoRepository<FollowingTopic, String> {
    fun findByUserId(ownerId: String): List<FollowingTopic>

    fun existsByUserIdAndTopicId(ownerId: String, topicId: String): Boolean

    fun findByUserIdAndTopicId(ownerId: String, topicId: String): List<FollowingTopic>
}
