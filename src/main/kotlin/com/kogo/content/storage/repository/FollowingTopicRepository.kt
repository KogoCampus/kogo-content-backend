package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.FollowingTopic
import org.springframework.data.mongodb.repository.MongoRepository

interface FollowingTopicRepository: MongoRepository<FollowingTopic, String> {
    fun findByOwnerId(ownerId: String): List<FollowingTopic>
}
