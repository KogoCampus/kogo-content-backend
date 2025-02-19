package com.kogo.content.service

import com.kogo.content.endpoint.common.FilterOperator
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.common.SortDirection
import com.kogo.content.endpoint.model.UserUpdate
import com.kogo.content.logging.Logger
import com.kogo.content.service.fileuploader.FileUploaderService
import com.kogo.content.storage.model.Notification
import com.kogo.content.storage.model.NotificationType
import com.kogo.content.storage.model.entity.Friend
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.repository.NotificationRepository
import com.kogo.content.storage.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService @Autowired constructor(
    private val userRepository: UserRepository,
    private val fileService: FileUploaderService,
    private val pushNotificationService: PushNotificationService,
    private val notificationRepository: NotificationRepository,
) : BaseEntityService<User, String>(User::class, userRepository) {

    companion object : Logger()

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

    fun create(username: String, email: String, schoolInfo: SchoolInfo): User {
        return userRepository.save(
            User(
                username = username,
                email = email,
                schoolInfo = schoolInfo,
            )
        )
    }

    @Transactional
    fun update(user: User, userUpdate: UserUpdate): User {
        with(userUpdate) {
            username?.let { user.username = it }
            pushToken?.let { user.pushNotificationToken = it }
            appLocalData?.let { user.appLocalData = it }
            profileImage?.let { it ->
                user.profileImage?.let { oldImage ->
                    runCatching { fileService.deleteFile(oldImage.id) }
                        .onFailure { log.error(it) { "Failed to delete old profile image: ${oldImage.id}" } }
                }
                user.profileImage = fileService.uploadFile(it)
            }
        }
        return userRepository.save(user)
    }

    fun deleteProfileImage(user: User): User {
        user.profileImage?.let { fileService.deleteFile(it.id) }
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

    @Transactional
    fun sendFriendRequest(user: User, targetUser: User, friendNickname: String): User {
        // Clean up old friend request notifications
        val notifications = pushNotificationService.getNotificationsByRecipientId(targetUser.id!!).filter {
            it.sender?.id == user.id && it.type == NotificationType.FRIEND_REQUEST
        }
        notifications.forEach { notificationRepository.delete(it) }

        pushNotificationService.sendPushNotification(
            Notification(
                type = NotificationType.FRIEND_REQUEST,
                recipient = targetUser,
                sender = user,
                title = "You have a friend request",
                body = "${user.email} would like to be your friend",
                deepLinkUrl = PushNotificationService.DeepLink.fallback
            )
        )

        if (!user.friends.any { it.user.id == targetUser.id }) {
            user.friends.add(Friend(targetUser, friendNickname, Friend.FriendStatus.PENDING))
        }
        return userRepository.save(user)
    }

    @Transactional
    fun acceptFriendRequest(user: User, requestedUser: User, friendNickname: String): User {
        // Clean up old friend request notifications
        val notification = pushNotificationService.getNotificationsByRecipientId(user.id!!).find {
            it.sender?.id == requestedUser.id && it.type == NotificationType.FRIEND_REQUEST
        }
        notification?.type = NotificationType.FRIEND_REQUEST_ACCEPTED
        notificationRepository.save(notification!!)

        // Update requested user's friend status to ACCEPTED
        val requestedUserFriend = requestedUser.friends.find { it.user.id == user.id }
        if (requestedUserFriend != null) {
            requestedUserFriend.status = Friend.FriendStatus.ACCEPTED
            userRepository.save(requestedUser)
        }

        // Update or add friend status for current user
        val existingFriend = user.friends.find { it.user.id == requestedUser.id }
        if (existingFriend != null) {
            existingFriend.status = Friend.FriendStatus.ACCEPTED
        } else {
            user.friends.add(Friend(requestedUser, friendNickname, Friend.FriendStatus.ACCEPTED))
        }

        // Send notification to the requested user
        pushNotificationService.sendPushNotification(
            Notification(
                type = NotificationType.GENERAL,
                recipient = requestedUser,
                sender = user,
                title = "Friend request accepted",
                body = "${user.email} has accepted your friend request",
                deepLinkUrl = PushNotificationService.DeepLink.fallback
            )
        )

        return userRepository.save(user)
    }

    fun updateLatestAccessTimestamp(user: User) = run {
        user.latestAccessTimestamp = System.currentTimeMillis()
        userRepository.save(user)
    }
}
