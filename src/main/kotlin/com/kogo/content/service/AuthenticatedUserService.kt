package com.kogo.content.service

import com.kogo.content.storage.entity.StudentUserEntity
import com.kogo.content.storage.repository.StudentUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

// Temporary file - to be modified or deleted
@Service
class AuthenticatedUserService @Autowired constructor(
    private val repository: StudentUserRepository
) {
    fun currentUserContext(): StudentUserEntity {
        val username = "testusername"
        return repository.findByUsername(username) ?: createUserProfileIfNotExist(username)
    }

    private fun createUserProfileIfNotExist(username: String): StudentUserEntity {
        return repository.save(StudentUserEntity(
            username = username,
            email = "testemail@gamil.com",
            schoolId = "sampleSchoolId"
        ))
    }
}
