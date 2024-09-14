package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.UserDetailsEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface UserDetailsRepository : MongoRepository<UserDetailsEntity, String> {
    fun findByUsername(username: String): UserDetailsEntity?

    fun existsByUsername(username: String): Boolean
}
