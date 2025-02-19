package com.kogo.content.service

import com.kogo.content.endpoint.model.UserUpdate
import com.kogo.content.service.fileuploader.FileUploaderService
import com.kogo.content.storage.model.Attachment
import com.kogo.content.storage.model.Notification
import com.kogo.content.storage.model.NotificationType
import com.kogo.content.storage.model.entity.Friend
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.repository.NotificationRepository
import com.kogo.content.storage.repository.UserRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.util.concurrent.CompletableFuture
import org.junit.jupiter.api.assertThrows
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

class UserServiceTest {
    private val userRepository: UserRepository = mockk()
    private val fileService: FileUploaderService = mockk()
    private val pushNotificationService: PushNotificationService = mockk()
    private val notificationRepository: NotificationRepository = mockk()

    private val userService = UserService(
        userRepository = userRepository,
        fileService = fileService,
        pushNotificationService = pushNotificationService,
        notificationRepository = notificationRepository,
    )

    private lateinit var user: User
    private lateinit var testImage: MockMultipartFile
    private lateinit var testAttachment: Attachment

    @BeforeEach
    fun setup() {
        user = User(
            id = "test-user-id",
            username = "testuser",
            email = "test@example.com",
            schoolInfo = SchoolInfo(
                schoolKey = "TEST",
                schoolName = "Test School",
                schoolShortenedName = "TS"
            ),
            profileImage = null
        )

        testImage = MockMultipartFile(
            "profileImage",
            "test-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".toByteArray()
        )

        testAttachment = Attachment(
            id = "test-image-id",
            filename = "test-image.jpg",
            contentType = MediaType.IMAGE_JPEG_VALUE,
            url = "test-url/test-image.jpg",
            size = 8000
        )

        // Mock security context for findCurrentUser
        val authentication: Authentication = mockk()
        val securityContext: SecurityContext = mockk()
        every { authentication.principal } returns user.username
        every { securityContext.authentication } returns authentication
        SecurityContextHolder.setContext(securityContext)
    }

    @Test
    fun `should find user by username`() {
        every { userRepository.findByUsername(user.username) } returns user

        val result = userService.findUserByUsername(user.username)

        assertThat(result).isEqualTo(user)
        verify { userRepository.findByUsername(user.username) }
    }

    @Test
    fun `should find user by email`() {
        every { userRepository.findByEmail(user.email) } returns user

        val result = userService.findUserByEmail(user.email)

        assertThat(result).isEqualTo(user)
        verify { userRepository.findByEmail(user.email) }
    }

    @Test
    fun `should find current user`() {
        every { userRepository.findByUsername(user.username) } returns user

        val result = userService.findCurrentUser()

        assertThat(result).isEqualTo(user)
        verify { userRepository.findByUsername(user.username) }
    }

    @Test
    fun `should create new user`() {
        val schoolInfo = SchoolInfo(
            schoolKey = "NEW",
            schoolName = "New School",
            schoolShortenedName = "NS"
        )

        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.create(
            username = "newuser",
            email = "newuser@example.com",
            schoolInfo = schoolInfo
        )

        assertThat(result.username).isEqualTo("newuser")
        assertThat(result.email).isEqualTo("newuser@example.com")
        assertThat(result.schoolInfo).isEqualTo(schoolInfo)
        verify { userRepository.save(any()) }
    }

    @Test
    fun `should update username only`() {
        val update = UserUpdate(
            username = "updated-username"
        )

        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.update(user, update)

        assertThat(result.username).isEqualTo(update.username)
        assertThat(result.profileImage).isNull()
        verify { userRepository.save(any()) }
    }

    @Test
    fun `should update profile image only`() {
        val update = UserUpdate(
            profileImage = testImage
        )

        every { fileService.uploadFile(testImage) } returns testAttachment
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.update(user, update)

        assertThat(result.username).isEqualTo(user.username)
        assertThat(result.profileImage).isEqualTo(testAttachment)
        verify {
            fileService.uploadFile(testImage)
            userRepository.save(any())
        }
    }

    @Test
    fun `should update both username and profile image`() {
        val update = UserUpdate(
            username = "updated-username",
            profileImage = testImage
        )

        every { fileService.uploadFile(testImage) } returns testAttachment
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.update(user, update)

        assertThat(result.username).isEqualTo(update.username)
        assertThat(result.profileImage).isEqualTo(testAttachment)
        verify {
            fileService.uploadFile(testImage)
            userRepository.save(any())
        }
    }

    @Test
    fun `should update profile image and delete old one`() {
        // Set existing profile image
        user.profileImage = testAttachment

        val newImage = MockMultipartFile(
            "profileImage",
            "new-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "new image content".toByteArray()
        )

        val newAttachment = testAttachment.copy(id = "new-image-id")
        val update = UserUpdate(
            profileImage = newImage
        )

        every { fileService.deleteFile(testAttachment.id) } just Runs
        every { fileService.uploadFile(newImage) } returns newAttachment
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.update(user, update)

        assertThat(result.profileImage).isEqualTo(newAttachment)
        verify {
            fileService.deleteFile(testAttachment.id)
            fileService.uploadFile(newImage)
            userRepository.save(any())
        }
    }

    @Test
    fun `should send friend request successfully`() {
        val targetUser = mockk<User>(relaxed = true) {
            every { id } returns "target-user-id"
            every { email } returns "target@example.com"
        }

        val existingNotifications = listOf(
            Notification(
                type = NotificationType.FRIEND_REQUEST,
                recipient = targetUser,
                sender = user,
                title = "Friend Request",
                body = "You have a friend request",
                deepLinkUrl = PushNotificationService.DeepLink.fallback
            )
        )

        every { pushNotificationService.getNotificationsByRecipientId("target-user-id") } returns existingNotifications
        every { notificationRepository.delete(any()) } just Runs
        every { pushNotificationService.sendPushNotification(any()) } returns CompletableFuture.completedFuture(mockk())
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.sendFriendRequest(user, targetUser, "test-nickname")

        assertThat(result.friends).hasSize(1)
        assertThat(result.friends[0].user.id).isEqualTo(targetUser.id)
        assertThat(result.friends[0].status).isEqualTo(Friend.FriendStatus.PENDING)

        verify(exactly = 1) {
            pushNotificationService.getNotificationsByRecipientId("target-user-id")
            notificationRepository.delete(existingNotifications[0])
            pushNotificationService.sendPushNotification(match { notification ->
                notification.recipient == targetUser &&
                notification.sender == user &&
                notification.type == NotificationType.FRIEND_REQUEST &&
                notification.title == "You have a friend request" &&
                notification.body == "${user.email} would like to be your friend"
            })
            userRepository.save(user)
        }
    }

    @Test
    fun `should not send duplicate friend request`() {
        val targetUser = mockk<User> {
            every { id } returns "target-user-id"
            every { email } returns "target@example.com"
        }

        // Add existing friend request
        user.friends.add(Friend(targetUser, "test-nickname", Friend.FriendStatus.PENDING))

        val existingNotifications = listOf(
            Notification(
                type = NotificationType.FRIEND_REQUEST,
                recipient = targetUser,
                sender = user,
                title = "Friend Request",
                body = "You have a friend request",
                deepLinkUrl = PushNotificationService.DeepLink.fallback
            )
        )

        every { pushNotificationService.getNotificationsByRecipientId("target-user-id") } returns existingNotifications
        every { notificationRepository.delete(any()) } just Runs
        every { pushNotificationService.sendPushNotification(any()) } returns CompletableFuture.completedFuture(mockk())
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.sendFriendRequest(user, targetUser, "test-nickname")

        assertThat(result.friends).hasSize(1)
        assertThat(result.friends[0].user.id).isEqualTo("target-user-id")
        assertThat(result.friends[0].status).isEqualTo(Friend.FriendStatus.PENDING)

        verify(exactly = 1) {
            pushNotificationService.getNotificationsByRecipientId("target-user-id")
            notificationRepository.delete(existingNotifications[0])
            pushNotificationService.sendPushNotification(any())
            userRepository.save(user)
        }
    }

    @Test
    fun `should accept friend request successfully`() {
        val requestedUser = mockk<User>(relaxed = true) {
            every { id } returns "requested-user-id"
            every { email } returns "requested@example.com"
            every { friends } returns mutableListOf(Friend(user, "test-nickname", Friend.FriendStatus.PENDING))
        }

        // Setup pending friend request
        user.friends.add(Friend(requestedUser, "test-nickname", Friend.FriendStatus.PENDING))

        val existingNotification = Notification(
            type = NotificationType.FRIEND_REQUEST,
            recipient = user,
            sender = requestedUser,
            title = "Friend Request",
            body = "You have a friend request",
            deepLinkUrl = PushNotificationService.DeepLink.fallback
        )

        every { pushNotificationService.getNotificationsByRecipientId(user.id!!) } returns listOf(existingNotification)
        every { notificationRepository.save(any()) } answers { firstArg() }
        every { userRepository.save(requestedUser) } returns requestedUser
        every { userRepository.save(user) } returns user
        every { pushNotificationService.sendPushNotification(any()) } returns CompletableFuture.completedFuture(mockk())

        val result = userService.acceptFriendRequest(user, requestedUser, "test-nickname")

        assertThat(result.friends).hasSize(1)
        assertThat(result.friends[0].user.id).isEqualTo(requestedUser.id)
        assertThat(result.friends[0].status).isEqualTo(Friend.FriendStatus.ACCEPTED)

        verify {
            pushNotificationService.getNotificationsByRecipientId(user.id!!)
            notificationRepository.save(match { notification ->
                notification.type == NotificationType.FRIEND_REQUEST_ACCEPTED &&
                notification.title == "Friend Request" &&
                notification.body == "You have a friend request"
            })
            userRepository.save(requestedUser)
            userRepository.save(user)
            pushNotificationService.sendPushNotification(match { notification ->
                notification.recipient == requestedUser &&
                notification.sender == user &&
                notification.type == NotificationType.GENERAL &&
                notification.title == "Friend request accepted" &&
                notification.body == "${user.email} has accepted your friend request"
            })
        }
    }

    @Test
    fun `should update existing friend status when accepting request`() {
        val requestedUser = mockk<User>(relaxed = true) {
            every { id } returns "requested-user-id"
            every { email } returns "requested@example.com"
            every { friends } returns mutableListOf(Friend(user, "test-nickname", Friend.FriendStatus.PENDING))
        }

        // Setup existing friend entries
        user.friends.add(Friend(requestedUser, "test-nickname", Friend.FriendStatus.PENDING))

        val existingNotification = Notification(
            type = NotificationType.FRIEND_REQUEST,
            recipient = user,
            sender = requestedUser,
            title = "Friend Request",
            body = "You have a friend request",
            deepLinkUrl = PushNotificationService.DeepLink.fallback
        )

        every { pushNotificationService.getNotificationsByRecipientId(user.id!!) } returns listOf(existingNotification)
        every { notificationRepository.save(any()) } answers { firstArg() }
        every { userRepository.save(requestedUser) } returns requestedUser
        every { userRepository.save(user) } returns user
        every { pushNotificationService.sendPushNotification(any()) } returns CompletableFuture.completedFuture(mockk())

        val result = userService.acceptFriendRequest(user, requestedUser, "test-nickname")

        assertThat(result.friends).hasSize(1)
        assertThat(result.friends[0].user.id).isEqualTo(requestedUser.id)
        assertThat(result.friends[0].status).isEqualTo(Friend.FriendStatus.ACCEPTED)

        verify {
            pushNotificationService.getNotificationsByRecipientId(user.id!!)
            notificationRepository.save(match { notification ->
                notification.type == NotificationType.FRIEND_REQUEST_ACCEPTED &&
                notification.title == "Friend Request" &&
                notification.body == "You have a friend request"
            })
            userRepository.save(requestedUser)
            userRepository.save(user)
            pushNotificationService.sendPushNotification(match { notification ->
                notification.recipient == requestedUser &&
                notification.sender == user &&
                notification.type == NotificationType.GENERAL &&
                notification.title == "Friend request accepted" &&
                notification.body == "${user.email} has accepted your friend request"
            })
        }
    }
}
