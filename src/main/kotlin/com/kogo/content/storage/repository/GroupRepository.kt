package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.GroupEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface GroupRepository : MongoRepository<GroupEntity, String> {
    fun findByGroupName(groupName: String): GroupEntity?
}