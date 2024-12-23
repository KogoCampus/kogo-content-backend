package com.kogo.content.service

import com.kogo.content.endpoint.model.UserUpdate
import com.kogo.content.logging.Logger
<<<<<<< HEAD
import com.kogo.content.storage.entity.User
import com.kogo.content.storage.repository.AttachmentRepository
=======
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
>>>>>>> 83a4b20 (fix: refactor entity structure and remove views)
import com.kogo.content.storage.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass

@Service
class UserService @Autowired constructor(
    private val userRepository: UserRepository
) : BaseEntityService<User, String>(User::class, userRepository) {
    companion object : Logger()

    fun findUserByUsername(username: String) = userRepository.findByUsername(username)

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
            // TODO
            // profileImage?.let { user.profileImage = attachmentRepository.saveFile(it) }
        }
        return userRepository.save(user)
    }
}
