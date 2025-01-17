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
import kotlin.reflect.KClass

@Service
class UserService @Autowired constructor(
    private val userRepository: UserRepository
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
            // TODO
            // profileImage?.let { user.profileImage = attachmentRepository.saveFile(it) }
        }
        return userRepository.save(user)
    }

//    fun addToBlacklist(itemType: BlacklistItem, itemId: String) = addToBlacklist(findCurrentUser(), itemType, itemId)
//    fun addToBlacklist(currentUser: User, itemType: BlacklistItem, itemId: String): User {
//        currentUser.blacklist.add(Pair(itemType, itemId))
//        return userRepository.save(currentUser)
//    }
//
//    fun removeFromBlacklist(itemType: BlacklistItem, itemId: String) = removeFromBlacklist(findCurrentUser(), itemType, itemId)
//    fun removeFromBlacklist(currentUser: User, itemType: BlacklistItem, itemId: String): User {
//        currentUser.blacklist.remove(Pair(itemType, itemId))
//        return userRepository.save(currentUser)
//    }
}
