package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.User
import com.kogo.content.storage.entity.UserIdToken
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface UserRepository : MongoRepository<User, String> {
    fun findByUsername(username: String): User?

    @Query("{ '_id': ?0 }")
    fun findUserById(id: String): User?

    fun existsByUsername(username: String): Boolean
}
