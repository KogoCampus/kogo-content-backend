package com.kogo.content.storage.repository

import com.kogo.content.storage.model.entity.User
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface UserRepository : MongoRepository<User, String> {
    fun findByUsername(username: String): User?
    fun findByEmail(email: String): User?

    @Query("{ '_id': ?0 }")
    fun findUserById(id: String): User?
}
