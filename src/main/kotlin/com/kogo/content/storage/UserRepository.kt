package com.kogo.content.storage

import com.kogo.content.storage.entity.User
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, String> {
}