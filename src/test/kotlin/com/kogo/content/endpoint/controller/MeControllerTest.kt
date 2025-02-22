package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.service.*
import com.kogo.test.util.Fixture
import com.kogo.content.storage.model.*
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.Post
import com.kogo.content.storage.model.entity.User
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.storage.model.entity.Friend
import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.*
import java.util.concurrent.CompletableFuture

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MeControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean private lateinit var userService: UserService
    @MockkBean private lateinit var groupService: GroupService
    @MockkBean private lateinit var postService: PostService
    @MockkBean private lateinit var pushNotificationService: PushNotificationService

    private lateinit var currentUser: User
    private lateinit var group: Group
    private lateinit var post: Post

    @BeforeEach
    fun setup() {
        currentUser = Fixture.createUserFixture()
        group = Fixture.createGroupFixture(owner = currentUser)
        post = Fixture.createPostFixture(group = group, author = currentUser)

        every { userService.findCurrentUser() } returns currentUser
    }

    @Test
    fun `should get current user info successfully`() {
        mockMvc.get("/me") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(currentUser.id) }
            jsonPath("$.data.username") { value(currentUser.username) }
            jsonPath("$.data.email") { value(currentUser.email) }
            jsonPath("$.data.schoolInfo.schoolName") { value(currentUser.schoolInfo.schoolName) }
        }
    }

    @Test
    fun `should update user info successfully`() {
        val updatedUsername = "updated-username"
        val updatedUser = currentUser.copy().apply {
            username = updatedUsername
        }

        every { userService.findUserByUsername(updatedUsername) } returns null
        every { userService.findUserByUsername(currentUser.username) } returns null
        every { userService.update(currentUser, any()) } returns updatedUser

        mockMvc.multipart("/me") {
            part(MockPart("username", updatedUsername.toByteArray()))
            with { it.method = "PUT"; it }
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.username") { value(updatedUsername) }
            jsonPath("$.data.email") { value(currentUser.email) }
        }
    }

    @Test
    fun `should block update user info when username is duplicate`() {
        val duplicateUsername = "duplicate-username"
        val duplicateUser = Fixture.createUserFixture().copy(username = duplicateUsername, id = "another-id")

        every { userService.findUserByUsername(duplicateUsername) } returns duplicateUser
        every { userService.findUserByUsername(currentUser.username) } returns currentUser

        mockMvc.multipart("/me") {
            part(MockPart("username", duplicateUsername.toByteArray()))
            with { it.method = "PUT"; it }
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error") { value("DUPLICATED") }
            jsonPath("$.details") { value("User with the given username already exists") }
        }

        verify(exactly = 0) { userService.update(any(), any()) }
    }

    @Test
    fun `should get user's owned posts`() {
        val userPosts = listOf(post)
        every { postService.findAllByAuthor(currentUser) } returns userPosts

        mockMvc.get("/me/ownership/posts") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(post.id) }
            jsonPath("$.data[0].author.id") { value(currentUser.id) }
        }
    }

    @Test
    fun `should get user's owned groups`() {
        val userGroups = listOf(group)
        every { groupService.findByOwner(currentUser) } returns userGroups

        mockMvc.get("/me/ownership/groups") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(group.id) }
            jsonPath("$.data[0].owner.id") { value(currentUser.id) }
        }
    }

    //@Test
    //fun `should transfer group ownership successfully`() {
    //    val newOwner = Fixture.createUserFixture()
    //    val updatedGroup = group.copy().apply {
    //        owner = newOwner
    //    }
    //
    //    every { groupService.findOrThrow(group.id!!) } returns group
    //    every { userService.findOrThrow(newOwner.id!!) } returns newOwner
    //    every { groupService.follow(group, newOwner) } returns true
    //    every { groupService.transferOwnership(group, newOwner) } returns updatedGroup
    //
    //    mockMvc.multipart("/me/ownership/groups/${group.id}/transfer") {
    //        part(MockPart("transfer_to", newOwner.id!!.toByteArray()))
    //    }.andExpect {
    //        status { isOk() }
    //        jsonPath("$.data.owner.id") { value(newOwner.id) }
    //    }
    //}

    @Test
    fun `should get user's following groups`() {
        val followingGroups = listOf(group)
        every { groupService.findAllByFollowerId(currentUser.id!!) } returns followingGroups

        mockMvc.get("/me/following") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(group.id) }
            jsonPath("$.data[0].followedByCurrentUser") { value(true) }
        }
    }

    @Test
    fun `should get user notifications with pagination`() {
        val sender = Fixture.createUserFixture()
        val notification = Notification(
            recipient = currentUser,
            sender = sender,
            title = "Test notification",
            body = "Test notification body",
            deepLinkUrl = "/"
        )

        val paginationSlice = PaginationSlice(items = listOf(notification))

        every { pushNotificationService.getNotificationsByRecipientId(currentUser.id!!, any()) } returns paginationSlice

        mockMvc.get("/me/notifications") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].recipient.id") { value(currentUser.id) }
            jsonPath("$.data[0].sender.id") { value(sender.id) }
            jsonPath("$.data[0].title") { value("Test notification") }
            jsonPath("$.data[0].body") { value("Test notification body") }
        }
    }

    @Test
    fun `should add user to blacklist successfully`() {
        val targetUser = Fixture.createUserFixture()
        val updatedUser = currentUser.copy().apply {
            blacklistUsers.add(targetUser)
        }

        every { userService.findOrThrow(targetUser.id!!) } returns targetUser
        every { userService.addUserToBlacklist(currentUser, targetUser) } returns updatedUser

        mockMvc.post("/me/blacklist") {
            param("user_id", targetUser.id!!)
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(currentUser.id) }
        }

        verify {
            userService.findOrThrow(targetUser.id!!)
            userService.addUserToBlacklist(currentUser, targetUser)
        }
    }

    @Test
    fun `should fail to add self to blacklist`() {
        every { userService.findOrThrow(any()) } returns currentUser

        mockMvc.post("/me/blacklist") {
            param("user_id", currentUser.id!!)
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
        }

        verify(exactly = 0) { userService.addUserToBlacklist(any(), any()) }
    }

    @Test
    fun `should handle non-existent user for blacklist`() {
        val nonExistentUserId = "non-existent-id"
        every { userService.findOrThrow(nonExistentUserId) } throws ResourceNotFoundException("User", nonExistentUserId)

        mockMvc.post("/me/blacklist") {
            param("user_id", nonExistentUserId)
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
        }

        verify(exactly = 0) { userService.addUserToBlacklist(any(), any()) }
    }

    @Test
    fun `should remove user from blacklist successfully`() {
        val targetUser = Fixture.createUserFixture()
        val updatedUser = currentUser.copy().apply {
            blacklistUsers.add(targetUser)
        }

        every { userService.findOrThrow(targetUser.id!!) } returns targetUser
        every { userService.removeUserFromBlacklist(currentUser, targetUser) } returns updatedUser

        mockMvc.delete("/me/blacklist/${targetUser.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `should delete profile image successfully`() {
        val updatedUser = currentUser.copy().apply {
            profileImage = null
        }

        every { userService.deleteProfileImage(currentUser) } returns updatedUser

        mockMvc.delete("/me/profileImage") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.profileImage") { doesNotExist() }
        }
    }

    @Test
    fun `should send friend request successfully`() {
        val targetUser = Fixture.createUserFixture().copy(
            id = "target-user-id",
            email = "target@example.com"
        )
        val friendNickname = "Friend Nick"

        every { userService.findUserByEmail(targetUser.email) } returns targetUser
        every { userService.sendFriendRequest(currentUser, targetUser, friendNickname) } just runs
        every { pushNotificationService.sendPushNotification(any()) } returns CompletableFuture.completedFuture(mockk())

        mockMvc.multipart("/me/friends") {
            part(MockPart("friendEmail", targetUser.email.toByteArray()))
            part(MockPart("friendNickname", friendNickname.toByteArray()))
        }.andExpect {
            status { isOk() }
        }

        verify {
            userService.sendFriendRequest(currentUser, targetUser, friendNickname)
        }
    }

    @Test
    fun `should not allow sending friend request to self`() {
        val friendNickname = "Self Nick"
        every { userService.findUserByEmail(currentUser.email) } returns currentUser

        mockMvc.multipart("/me/friends") {
            part(MockPart("friendEmail", currentUser.email.toByteArray()))
            part(MockPart("friendNickname", friendNickname.toByteArray()))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("BAD_REQUEST") }
            jsonPath("$.details") { value("You cannot send yourself to friend") }
        }

        verify(exactly = 0) { userService.sendFriendRequest(any(), any(), any()) }
    }

    @Test
    fun `should handle non-existent user for friend request`() {
        val nonExistentEmail = "nonexistent@example.com"
        val friendNickname = "Non Existent Nick"
        every { userService.findUserByEmail(nonExistentEmail) } returns null

        mockMvc.multipart("/me/friends") {
            part(MockPart("friendEmail", nonExistentEmail.toByteArray()))
            part(MockPart("friendNickname", friendNickname.toByteArray()))
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("NOT_FOUND") }
            jsonPath("$.details") { value("User not found for the given email $nonExistentEmail") }
        }

        verify(exactly = 0) { userService.sendFriendRequest(any(), any(), any()) }
    }

    @Test
    fun `should accept friend request successfully`() {
        // Create mock users
        val requestedUser = mockk<User>(relaxed = true) {
            every { id } returns "requested-user-id"
        }
        val friendNickname = "Accepted Nick"
        val newFriend = Friend(
            friendUserId = "requested-user-id",
            nickname = friendNickname,
            status = Friend.FriendStatus.ACCEPTED,
        )

        // Mock currentUser with empty friends list initially
        currentUser = mockk(relaxed = true) {
            every { id } returns "current-user-id"
            every { friends } returns mutableListOf()
        }

        // Mock requested user with a pending friend request to current user
        every { requestedUser.friends } returns mutableListOf(
            Friend(currentUser.id!!, "test-friend-name", Friend.FriendStatus.PENDING)
        )

        every { userService.findCurrentUser() } returns currentUser
        every { userService.find("requested-user-id") } returns requestedUser
        every { userService.acceptFriendRequest(currentUser, requestedUser, friendNickname) } returns newFriend
        every { pushNotificationService.sendPushNotification(any()) } returns CompletableFuture.completedFuture(mockk())

        mockMvc.multipart("/me/friends/accept") {
            part(MockPart("requestedUserId", requestedUser.id!!.toByteArray()))
            part(MockPart("friendNickname", friendNickname.toByteArray()))
            with { it.method = "PUT"; it }
        }.andExpect {
            status { isOk() }
        }

        verify {
            userService.acceptFriendRequest(currentUser, requestedUser, friendNickname)
        }
    }

    @Test
    fun `should not accept friend request when no pending request exists`() {
        val requestedUser = mockk<User>(relaxed = true) {
            every { id } returns "requested-user-id"
            every { friends } returns mutableListOf()
        }
        val friendNickname = "No Request Nick"

        // Mock currentUser with empty friends list
        currentUser = mockk(relaxed = true) {
            every { id } returns "current-user-id"
            every { friends } returns mutableListOf()
        }

        every { userService.findCurrentUser() } returns currentUser
        every { userService.find("requested-user-id") } returns requestedUser

        mockMvc.multipart("/me/friends/accept") {
            part(MockPart("requestedUserId", requestedUser.id!!.toByteArray()))
            part(MockPart("friendNickname", friendNickname.toByteArray()))
            with { it.method = "PUT"; it }
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("BAD_REQUEST") }
            jsonPath("$.details") { value("This user has not made a friend request for you") }
        }

        verify(exactly = 0) { userService.acceptFriendRequest(any(), any(), any()) }
    }

    @Test
    fun `should not accept friend request when already accepted`() {
        // Create mock users
        val requestedUser = mockk<User>(relaxed = true) {
            every { id } returns "requested-user-id"
        }
        val friendNickname = "Already Accepted Nick"

        // Mock currentUser with empty friends list
        currentUser = mockk(relaxed = true) {
            every { id } returns "current-user-id"
            every { friends } returns mutableListOf()
        }

        // Mock requested user with an already accepted friend relationship
        every { requestedUser.friends } returns mutableListOf(
            Friend(currentUser.id!!, "test-friend-name", Friend.FriendStatus.ACCEPTED)
        )

        every { userService.findCurrentUser() } returns currentUser
        every { userService.find("requested-user-id") } returns requestedUser

        mockMvc.multipart("/me/friends/accept") {
            part(MockPart("requestedUserId", requestedUser.id!!.toByteArray()))
            part(MockPart("friendNickname", friendNickname.toByteArray()))
            with { it.method = "PUT"; it }
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("BAD_REQUEST") }
            jsonPath("$.details") { value("User has already accepted a friend request for you") }
        }

        verify(exactly = 0) { userService.acceptFriendRequest(any(), any(), any()) }
    }

    @Test
    fun `should delete notification successfully`() {
        val notification = Notification(
            id = "test-notification-id",
            recipient = currentUser,
            sender = Fixture.createUserFixture(),
            title = "Test notification",
            body = "Test body",
            deepLinkUrl = "/"
        )

        every { pushNotificationService.find(notification.id!!) } returns notification
        every { pushNotificationService.deleteNotification(notification.id!!, currentUser.id!!) } just runs

        mockMvc.delete("/me/notifications/${notification.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }

        verify {
            pushNotificationService.deleteNotification(notification.id!!, currentUser.id!!)
        }
    }

    @Test
    fun `should not delete notification that belongs to another user`() {
        val otherUser = Fixture.createUserFixture().copy(id = "other-user-id")
        val notification = Notification(
            id = "test-notification-id",
            recipient = otherUser,
            sender = Fixture.createUserFixture(),
            title = "Test notification",
            body = "Test body",
            deepLinkUrl = "/"
        )

        every { pushNotificationService.find(notification.id!!) } returns notification

        mockMvc.delete("/me/notifications/${notification.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error") { value("UNAUTHORIZED") }
            jsonPath("$.details") { value("Notification does not belong to the user") }
        }

        verify(exactly = 0) { pushNotificationService.deleteNotification(any(), any()) }
    }

    @Test
    fun `should handle non-existent notification deletion`() {
        val notificationId = "non-existent-id"
        every { pushNotificationService.find(notificationId) } returns null

        mockMvc.delete("/me/notifications/$notificationId") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("NOT_FOUND") }
            jsonPath("$.details") { value("Notification not found for the given notificationId $notificationId") }
        }

        verify(exactly = 0) { pushNotificationService.deleteNotification(any(), any()) }
    }
}
