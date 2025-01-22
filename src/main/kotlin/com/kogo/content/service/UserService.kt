package com.kogo.content.service

import com.kogo.content.endpoint.model.UserUpdate
import com.kogo.content.logging.Logger
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService @Autowired constructor(
    private val userRepository: UserRepository,
    private val fileService: FileUploaderService
) : BaseEntityService<User, String>(User::class, userRepository) {
    companion object : Logger()

    fun findUserByUsername(username: String) = userRepository.findByUsername(username)
    fun findUserByEmail(email: String) = userRepository.findByEmail(email)

    fun findCurrentUser(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication.principal as String
        return findUserByUsername(username) ?: throw RuntimeException("Username not found $username")
    }

    fun create(username: String, email: String, schoolInfo: SchoolInfo): User =
        userRepository.save(
            User(
            username = username,
            email = email,
            schoolInfo = schoolInfo
            )
        )

    @Transactional
    fun update(user: User, userUpdate: UserUpdate): User {
        with(userUpdate) {
            username?.let { user.username = it }
            pushToken?.let { user.pushNotificationToken = it }
            profileImage?.let { it ->
                user.profileImage?.let { oldImage ->
                    runCatching { fileService.deleteImage(oldImage.id) }
                        .onFailure { log.error(it) { "Failed to delete old profile image: ${oldImage.id}" } }
                }
                user.profileImage = fileService.uploadImage(it)
            }
        }
        return userRepository.save(user)
    }

    fun deleteProfileImage(user: User): User {
        user.profileImage?.let { fileService.deleteImage(it.id) }
        user.profileImage = null
        return userRepository.save(user)
    }

    fun addUserToBlacklist(user: User, targetUser: User): User? {
        if (!user.blacklistUserIds.contains(targetUser.id)) {
            user.blacklistUserIds.add(targetUser.id!!)
            return userRepository.save(user)
        }
        return null
    }

    fun removeUserFromBlacklist(user: User, targetUser: User): User? {
        user.blacklistUserIds.remove(targetUser.id)
        return userRepository.save(user)
    }
}
