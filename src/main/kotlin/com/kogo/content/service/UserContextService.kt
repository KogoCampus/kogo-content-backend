package com.kogo.content.service

import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.UserDetailsRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

// Temporary file - to be modified or deleted
@Service
class UserContextService @Autowired constructor(
    private val userDetailsRepository: UserDetailsRepository
) {
    fun getCurrentUsername(): String {
        val context = SecurityContextHolder.getContext().authentication
        val jwt = (context.principal as Jwt).claims
        val username = jwt["username"] as String
        return username
    }

    fun findUserProfileByUsername(username: String): UserDetails? =
        userDetailsRepository.findByUsername(username)

    fun existsUserProfileByUsername(username: String) = userDetailsRepository.existsByUsername(username)

    fun createUserProfile(username: String, email: String): UserDetails =
        userDetailsRepository.save(UserDetails(
            username = username,
            email = email
        ))
}
