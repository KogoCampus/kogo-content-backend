package com.kogo.content.service

import com.kogo.content.endpoint.model.UserUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.logging.Logger
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.PostRepository
import com.kogo.content.storage.repository.TopicRepository
import com.kogo.content.storage.repository.UserDetailsRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Temporary file - to be modified or deleted
@Service
class UserContextService @Autowired constructor(
    private val userDetailsRepository: UserDetailsRepository,
    private val attachmentRepository: AttachmentRepository,
    private val postRepository: PostRepository,
    private val topicRepository: TopicRepository,
    private val fileHandler: FileHandler,
) {
    companion object : Logger()

    fun getCurrentUsername(): String {
        val context = SecurityContextHolder.getContext().authentication
        val jwt = (context.principal as Jwt).claims
        return jwt["username"] as String
    }

    fun getCurrentUserDetails(): UserDetails {
        val username = getCurrentUsername()
        return findUserProfileByUsername(username) ?: throw RuntimeException("Username not found $username")
    }

    fun findUserProfileByUsername(username: String) = userDetailsRepository.findByUsername(username)

    fun existsUserProfileByUsername(username: String) = userDetailsRepository.existsByUsername(username)

    fun createUserProfile(username: String, email: String): UserDetails =
        userDetailsRepository.save(UserDetails(
            username = username,
            email = email
        ))

    @Transactional
    fun updateUserProfile(user: UserDetails, userUpdate: UserUpdate): UserDetails {
        with(userUpdate){
            username?.let { user.username = it }
            profileImage?.let { user.profileImage = attachmentRepository.saveFileAndReturnAttachment(it, fileHandler, attachmentRepository)  }
        }
        return userDetailsRepository.save(user)
    }

    fun getUserPosts(user: UserDetails): List<Post> {
        val userId = user.id!!
        return postRepository.findAllByAuthorId(userId)
    }

    fun getUserTopics(user: UserDetails): List<Topic> {
        val userId = user.id!!
        return topicRepository.findAllByOwnerId(userId)
    }

    fun getUserFollowings(user: UserDetails): List<Topic> {
       return user.followingTopics!!
    }
}
