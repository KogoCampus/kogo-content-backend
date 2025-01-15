package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.service.*
import com.kogo.test.util.Fixture
import com.kogo.content.storage.model.*
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.Post
import com.kogo.content.storage.model.entity.User
import com.kogo.content.endpoint.model.UserData
import com.kogo.content.storage.model.entity.BlacklistItem
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MeControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean private lateinit var userService: UserService
    @MockkBean private lateinit var groupService: GroupService
    @MockkBean private lateinit var postService: PostService
    @MockkBean private lateinit var notificationService: NotificationService

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

    @Test
    fun `should transfer group ownership successfully`() {
        val newOwner = Fixture.createUserFixture()
        val updatedGroup = group.copy().apply {
            owner = newOwner
        }

        every { groupService.findOrThrow(group.id!!) } returns group
        every { userService.findOrThrow(newOwner.id!!) } returns newOwner
        every { groupService.follow(group, newOwner) } returns true
        every { groupService.transferOwnership(group, newOwner) } returns updatedGroup

        mockMvc.multipart("/me/ownership/groups/${group.id}/transfer") {
            part(MockPart("transfer_to", newOwner.id!!.toByteArray()))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.owner.id") { value(newOwner.id) }
        }
    }

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
    fun `should update push token successfully`() {
        val pushToken = "new-push-token"
        val updatedUser = currentUser.copy().apply {
            pushNotificationToken = pushToken
        }

        every { notificationService.updatePushToken(currentUser.id!!, pushToken) } returns updatedUser

        mockMvc.put("/me/push-token?push_token=$pushToken") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.pushNotificationToken") { value(pushToken) }
        }
    }

    @Test
    fun `should get user notifications with pagination`() {
        val sender = Fixture.createUserFixture()
        val notifications = listOf(
            Notification(
                recipientId = currentUser.id!!,
                sender = UserData.Public.from(sender),
                eventType = EventType.LIKE_TO_POST,
                message = NotificationMessage(
                    title = "New Like",
                    body = "${sender.username} liked your post",
                    dataType = DataType.POST,
                    data = post
                ),
                isPushNotification = true,
            )
        )
        val paginationSlice = PaginationSlice(items = notifications)

        every { notificationService.getNotificationsByRecipientId(currentUser.id!!, any()) } returns paginationSlice

        mockMvc.get("/me/notifications") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].recipientId") { value(currentUser.id) }
            jsonPath("$.data[0].eventType") { value(EventType.LIKE_TO_POST.name) }
            jsonPath("$.data[0].message.title") { value("New Like") }
            jsonPath("$.data[0].message.dataType") { value(DataType.POST.name) }
        }
    }

    @Test
    fun `should add user to blacklist successfully`() {
        val targetUser = Fixture.createUserFixture()

        every { userService.findOrThrow(targetUser.id!!) } returns targetUser
        every { userService.addToBlacklist(currentUser, BlacklistItem.User, targetUser.id!!) } returns currentUser

        mockMvc.post("/me/blacklist/users/${targetUser.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(currentUser.id) }
        }

        verify { userService.addToBlacklist(currentUser, BlacklistItem.User, targetUser.id!!) }
    }

    @Test
    fun `should fail to add self to blacklist`() {
        every { userService.findOrThrow(currentUser.id!!) } returns currentUser

        mockMvc.post("/me/blacklist/users/${currentUser.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value(ErrorCode.BAD_REQUEST.name) }
            jsonPath("$.details") { value("Cannot blacklist yourself") }
        }

        verify(exactly = 0) { userService.addToBlacklist(any(), any(), any()) }
    }

    @Test
    fun `should remove user from blacklist successfully`() {
        val targetUser = Fixture.createUserFixture()

        every { userService.findOrThrow(targetUser.id!!) } returns targetUser
        every { userService.removeFromBlacklist(currentUser, BlacklistItem.User, targetUser.id!!) } returns currentUser

        mockMvc.delete("/me/blacklist/users/${targetUser.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(currentUser.id) }
        }

        verify { userService.removeFromBlacklist(currentUser, BlacklistItem.User, targetUser.id!!) }
    }

    @Test
    fun `should add post to blacklist successfully`() {
        val targetPost = Fixture.createPostFixture(group = group, author = Fixture.createUserFixture())

        every { postService.findOrThrow(targetPost.id!!) } returns targetPost
        every { userService.addToBlacklist(currentUser, BlacklistItem.Post, targetPost.id!!) } returns currentUser

        mockMvc.post("/me/blacklist/posts/${targetPost.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(currentUser.id) }
        }

        verify { userService.addToBlacklist(currentUser, BlacklistItem.Post, targetPost.id!!) }
    }

    @Test
    fun `should fail to blacklist own post`() {
        val ownPost = Fixture.createPostFixture(group = group, author = currentUser)

        every { postService.findOrThrow(ownPost.id!!) } returns ownPost

        mockMvc.post("/me/blacklist/posts/${ownPost.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value(ErrorCode.BAD_REQUEST.name) }
            jsonPath("$.details") { value("Cannot blacklist your own post") }
        }

        verify(exactly = 0) { userService.addToBlacklist(any(), any(), any()) }
    }

    @Test
    fun `should remove post from blacklist successfully`() {
        val targetPost = Fixture.createPostFixture(group = group, author = Fixture.createUserFixture())

        every { userService.removeFromBlacklist(currentUser, BlacklistItem.Post, targetPost.id!!) } returns currentUser

        mockMvc.delete("/me/blacklist/posts/${targetPost.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(currentUser.id) }
        }

        verify { userService.removeFromBlacklist(currentUser, BlacklistItem.Post, targetPost.id!!) }
    }

    @Test
    fun `should add comment to blacklist successfully`() {
        val commentId = "test-comment-id"
        every { userService.addToBlacklist(currentUser, BlacklistItem.Comment, commentId) } returns currentUser

        mockMvc.post("/me/blacklist/comments/$commentId") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(currentUser.id) }
        }

        verify { userService.addToBlacklist(currentUser, BlacklistItem.Comment, commentId) }
    }

    @Test
    fun `should remove comment from blacklist successfully`() {
        val commentId = "test-comment-id"
        every { userService.removeFromBlacklist(currentUser, BlacklistItem.Comment, commentId) } returns currentUser

        mockMvc.delete("/me/blacklist/comments/$commentId") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(currentUser.id) }
        }

        verify { userService.removeFromBlacklist(currentUser, BlacklistItem.Comment, commentId) }
    }
}
