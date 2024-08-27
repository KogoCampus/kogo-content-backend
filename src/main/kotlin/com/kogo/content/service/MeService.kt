package com.kogo.content.service

import com.kogo.content.service.filehandler.FileHandlerService
import com.kogo.content.storage.entity.GroupEntity
import com.kogo.content.storage.entity.PostEntity
import com.kogo.content.storage.entity.UserEntity
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.GroupRepository
import com.kogo.content.storage.repository.PostRepository
import com.kogo.content.storage.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class MeService @Autowired constructor(
    private val fileHandlerService: FileHandlerService,
    private val groupRepository: GroupRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val attachmentRepository: AttachmentRepository,
    private val groupService: GroupService,
    private val postService: PostService,
    private val userService: UserService,
    private val attachmentService: AttachmentService
) {
    // TO BE MODIFIED
    fun getUser(): UserEntity? {
        // TO BE MODIFIED
        return userService.findUser("testUser")
    }

    // TO BE MODIFIED
    fun updateUserDetails(attributes: Map<String, Any?>): UserEntity? {
        // TO BE MODIFIED
        val updatingUser = userService.findUser("testUser")!!
        attributes.forEach { (key, value) ->
            when (key) {
                "username" -> if (value is String) updatingUser.username = value
                "profileImage" -> if (value is MultipartFile) {
                    updatingUser.profileImage?.let { currentProfileImage ->
                            currentProfileImage.user = null
                            attachmentRepository.save(currentProfileImage)
                    }
                    val newAttachment = attachmentService.saveAttachment(value, updatingUser.id, "user")
                    updatingUser.profileImage = newAttachment
                }
            }
        }
        return userRepository.save(updatingUser)
    }

    // TO BE MODIFIED
    fun getUserPosts(): List<PostEntity>?{
        // TO BE MODIFIED
        val userId = userService.findUser("testUser")?.id!!
        return postRepository.findByAuthorId(userId)
    }

    // TO BE MODIFIED
    fun getUserGroups(): List<GroupEntity>?{
        // TO BE MODIFIED
        val userId = userService.findUser("testUser")?.id!!
        return groupRepository.findByOwnerId(userId)
    }

    // TO BE MODIFIED
    fun getUserFollowing(): List<GroupEntity>?{
        // TO BE MODIFIED
        val user = userService.findUser("testUser")
        return user?.following ?: emptyList()
    }
}