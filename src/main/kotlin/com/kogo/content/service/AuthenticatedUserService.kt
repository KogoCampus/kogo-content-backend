package com.kogo.content.service

import com.kogo.content.storage.entity.StudentUserEntity
import com.kogo.content.storage.repository.StudentUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

// Temporary file - to be modified or deleted
@Service
class AuthenticatedUserService @Autowired constructor(
    private val repository: StudentUserRepository
) {
    fun getCurrentAuthenticatedUser(): StudentUserEntity {
        val context = SecurityContextHolder.getContext().authentication
        val jwt = (context.principal as Jwt).claims
        val username = jwt["username"] as String
        return repository.findByUsername(username) ?: throw RuntimeException("Failed to find a corresponding user profile for the username $username")
    }

    fun createUserProfile(username: String, email: String): StudentUserEntity {
        return repository.save(StudentUserEntity(
            username = username,
            email = email
        ))
    }
}
