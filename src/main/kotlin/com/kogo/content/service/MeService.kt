package com.kogo.content.service

import com.kogo.content.service.filehandler.FileHandlerService
import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.storage.entity.PostEntity
import com.kogo.content.storage.entity.StudentUserEntity
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.TopicRepository
import com.kogo.content.storage.repository.PostRepository
import com.kogo.content.storage.repository.StudentUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class MeService @Autowired constructor(
    private val fileHandlerService: FileHandlerService,
    private val topicRepository: TopicRepository,
    private val postRepository: PostRepository,
    private val userRepository: StudentUserRepository,
    private val attachmentRepository: AttachmentRepository,
    private val topicService: TopicService,
    private val postService: PostService,
    private val authenticatedUserService: AuthenticatedUserService,
    private val attachmentService: AttachmentService
) {
    // TO BE MODIFIED
    fun getUser(): StudentUserEntity? {
        // TO BE MODIFIED
        return authenticatedUserService.findUser("testUser")
    }

    // TO BE MODIFIED
    fun updateUserDetails(attributes: Map<String, Any?>): StudentUserEntity? {
        // TO BE MODIFIED
        val updatingUser = authenticatedUserService.findUser("testUser")!!
        attributes.forEach { (key, value) ->
            when (key) {
                "username" -> if (value is String) updatingUser.username = value
                "profileImage" -> if (value is MultipartFile) {
                    updatingUser.profileImage?.let { currentProfileImage ->
                            currentProfileImage.parent = null
                            attachmentRepository.save(currentProfileImage)
                    }
                    val newAttachment = attachmentService.saveAttachment(value, updatingUser.id)
                    updatingUser.profileImage = newAttachment
                }
            }
        }
        return userRepository.save(updatingUser)
    }

    // TO BE MODIFIED
    fun getUserPosts(): List<PostEntity>?{
        // TO BE MODIFIED
        val userId = authenticatedUserService.findUser("testUser")?.id!!
        return postRepository.findByAuthorId(userId)
    }

    // TO BE MODIFIED
    fun getUserGroups(): List<TopicEntity>?{
        // TO BE MODIFIED
        val userId = authenticatedUserService.findUser("testUser")?.id!!
        return topicRepository.findByOwnerId(userId)
    }

    // TO BE MODIFIED
    fun getUserFollowing(): List<TopicEntity>?{
        // TO BE MODIFIED
        val user = authenticatedUserService.findUser("testUser")
        return user?.followingTopics ?: emptyList()
    }
}