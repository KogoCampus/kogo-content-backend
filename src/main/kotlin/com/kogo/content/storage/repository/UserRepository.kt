package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.User
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, String> {
    fun findByUsername(username: String): User?

    fun existsByUsername(username: String): Boolean
}
