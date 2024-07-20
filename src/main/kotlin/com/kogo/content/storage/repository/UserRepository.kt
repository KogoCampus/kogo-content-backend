package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.UserEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<UserEntity, String> {
}