package com.kogo.content.service

import com.kogo.content.common.PaginationRequest
import com.kogo.content.endpoint.model.UserUpdate
import com.kogo.content.logging.Logger
import com.kogo.content.storage.entity.User
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Temporary file - to be modified or deleted
@Service
class UserService @Autowired constructor(
    private val userRepository: UserRepository,
    private val attachmentRepository: AttachmentRepository,
) {
    companion object : Logger()

    fun find(userId: String): User? = userRepository.findByIdOrNull(userId)
    fun findUserByUsername(username: String) = userRepository.findByUsername(username)

    fun getCurrentUser(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication.principal as String
        return findUserByUsername(username) ?: throw RuntimeException("Username not found $username")
    }

    fun getAllUsersFollowingTopic(topicId: String, paginationRequest: PaginationRequest)
        = userRepository.findUsersByTopicId(topicId, paginationRequest)

    fun createUser(username: String, email: String, schoolName: String = "", schoolShortenedName: String = ""): User =
        userRepository.save(User(
            username = username,
            email = email,
            schoolName = schoolName,
            schoolShortenedName = schoolShortenedName
        ))

    @Transactional
    fun updateUser(user: User, userUpdate: UserUpdate): User {
        with(userUpdate) {
            username?.let { user.username = it }
            profileImage?.let { user.profileImage = attachmentRepository.saveFile(it) }
        }
        return userRepository.save(user)
    }
}
