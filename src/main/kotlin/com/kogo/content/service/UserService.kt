package com.kogo.content.service

import com.kogo.content.endpoint.common.FilterOperator
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.common.SortDirection
import com.kogo.content.endpoint.model.UserUpdate
import com.kogo.content.logging.Logger
import com.kogo.content.service.fileuploader.FileUploaderService
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.repository.UserRepository
import com.kogo.content.util.convertTo12BytesHexString
import jakarta.annotation.PostConstruct
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService @Autowired constructor(
    private val userRepository: UserRepository,
    private val fileService: FileUploaderService,
) : BaseEntityService<User, String>(User::class, userRepository) {

    companion object : Logger()

    private val systemUserId = convertTo12BytesHexString("system_user")

    @PostConstruct
    fun createSystemUserIfNotExists() {
        val systemUser = mongoTemplate.findOne(
            Query.query(Criteria.where("_id").`is`(systemUserId)),
            User::class.java
        )

        if (systemUser == null) {
            log.info { "Creating system user..." }
            try {
                mongoTemplate.save(
                    User(
                        id = systemUserId,
                        username = "system_user",
                        email = "op@sfu.ca",
                        schoolInfo = SchoolInfo(
                            schoolKey = "system",
                            schoolName = "System",
                            schoolShortenedName = null,
                        ),
                        followingGroupIds = mutableListOf()
                    )
                ).also { log.info { "System user created successfully" } }
            } catch (e: Exception) {
                log.error(e) { "Failed to create system user" }
                throw e
            }
        }
    }

    fun getSystemUser() = userRepository.findByIdOrNull(systemUserId)!!

    fun findUserByUsername(username: String) = userRepository.findByUsername(username)
    fun findUserByEmail(email: String) = userRepository.findByEmail(email)

    fun findCurrentUser(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication.principal as String
        return findUserByUsername(username) ?: throw RuntimeException("Username not found $username")
    }

    fun findAllFollowersByGroup(group: Group, paginationRequest: PaginationRequest): PaginationSlice<User> =
        mongoPaginationQueryBuilder.getPage(
            entityClass = User::class,
            paginationRequest = paginationRequest
                .withFilter("followingGroupIds", ObjectId(group.id!!), FilterOperator.IN)
                .withSort("username", SortDirection.ASC)
        )

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

    fun addUserToBlacklist(user: User, targetUser: User): User {
        if (!user.blacklistUsers.any { it.id == targetUser.id }) {
            user.blacklistUsers.add(targetUser)
        }
        return userRepository.save(user)
    }

    fun removeUserFromBlacklist(user: User, targetUser: User): User {
        user.blacklistUsers.remove(targetUser)
        return userRepository.save(user)
    }
}
