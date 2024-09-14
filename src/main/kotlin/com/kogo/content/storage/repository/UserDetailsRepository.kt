package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.UserDetails
import org.springframework.data.mongodb.repository.MongoRepository

interface UserDetailsRepository : MongoRepository<UserDetails, String> {
    fun findByUsername(username: String): UserDetails?

    fun existsByUsername(username: String): Boolean
}
