package com.kogo.content.storage.repository

import com.kogo.content.storage.model.entity.Group
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface GroupRepository : MongoRepository<Group, String> {
    fun findByGroupName(topicName: String): Group?
    fun findAllByOwnerId(ownerId: String): List<Group>

    @Query("{ 'followerIds': ?0 }")
    fun findAllByFollowerId(followerId: String): List<Group>
}
