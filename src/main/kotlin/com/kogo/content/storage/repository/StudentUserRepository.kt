package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.StudentUserEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface StudentUserRepository : MongoRepository<StudentUserEntity, String> {
    fun findByUsername(username: String): StudentUserEntity?
}