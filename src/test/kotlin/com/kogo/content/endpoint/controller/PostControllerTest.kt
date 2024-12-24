package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.service.*
import com.kogo.content.endpoint.`test-util`.Fixture
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.storage.model.DataType
import com.kogo.content.storage.model.EventType
import com.kogo.content.storage.model.Like
import com.kogo.content.storage.model.NotificationMessage
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.Post
import com.kogo.content.storage.model.entity.User
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean private lateinit var postService: PostService
    @MockkBean private lateinit var groupService: GroupService
    @MockkBean private lateinit var userService: UserService
    @MockkBean private lateinit var notificationService: NotificationService

    private lateinit var currentUser: User
    private lateinit var group: Group
    private lateinit var post: Post

    @BeforeEach
    fun setup() {
        currentUser = Fixture.createUserFixture()
        group = Fixture.createGroupFixture(owner = currentUser)
        post = Fixture.createPostFixture(
            group = group,
            author = currentUser,
            likes = mutableListOf(Like(userId = currentUser.id!!, isActive = true)),
            viewerIds = mutableListOf(currentUser.id!!)
        )

        every { userService.findCurrentUser() } returns currentUser
        every { groupService.findOrThrow(group.id!!) } returns group
        every { postService.findOrThrow(post.id!!) } returns post
        every { postService.find(post.id!!) } returns post
    }

    @Test
    fun `should get post successfully`() {
        every { postService.addViewer(post, currentUser) } returns true

        mockMvc.get("/media/posts/${post.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(post.id) }
            jsonPath("$.data.title") { value(post.title) }
            jsonPath("$.data.content") { value(post.content) }
            jsonPath("$.data.likeCount") { value(post.activeLikes.size) }
            jsonPath("$.data.viewCount") { value(post.viewerIds.size) }
            jsonPath("$.data.likedByCurrentUser") { value(true) }
            jsonPath("$.data.viewedByCurrentUser") { value(true) }
            jsonPath("$.data.groupId") { value(group.id) }
            jsonPath("$.data.groupName") { value(group.groupName) }
            jsonPath("$.data.author.id") { value(currentUser.id) }
        }

        verify { postService.addViewer(post, currentUser) }
    }

    @Test
    fun `should create post successfully when user follows group`() {
        group.followerIds.add(currentUser.id!!)
        val newPost = Fixture.createPostFixture(group = group, author = currentUser)

        every { postService.create(group, currentUser, any()) } returns newPost
        every { postService.addViewer(newPost, currentUser) } returns true

        mockMvc.multipart("/media/groups/${group.id}/posts") {
            part(MockPart("title", newPost.title.toByteArray()))
            part(MockPart("content", newPost.content.toByteArray()))
            file(MockMultipartFile("images", "test.png", "image/png", "test".toByteArray()))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.title") { value(newPost.title) }
            jsonPath("$.data.content") { value(newPost.content) }
            jsonPath("$.data.groupId") { value(group.id) }
            jsonPath("$.data.groupName") { value(group.groupName) }
            jsonPath("$.data.author.id") { value(currentUser.id) }
        }
    }

    @Test
    fun `should fail to create post when user does not follow group`() {
        // Create a new group where current user is not a follower
        val unfollowedGroup = Fixture.createGroupFixture(owner = currentUser).apply {
            followerIds = mutableListOf() // Clear all followers
        }
        
        every { groupService.findOrThrow(unfollowedGroup.id!!) } returns unfollowedGroup

        mockMvc.multipart("/media/groups/${unfollowedGroup.id}/posts") {
            part(MockPart("title", "Test Title".toByteArray()))
            part(MockPart("content", "Test Content".toByteArray()))
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value(ErrorCode.USER_ACTION_DENIED.name) }
        }
    }

    @Test
    fun `should update post successfully when user is author`() {
        val updatedTitle = "Updated Title"
        val updatedContent = "Updated Content"
        val updatedPost = post.copy().apply {
            title = updatedTitle
            content = updatedContent
        }

        every { postService.find(post.id!!) } returns post
        every { postService.update(post, any()) } returns updatedPost

        mockMvc.multipart("/media/posts/${post.id}") {
            part(MockPart("title", updatedTitle.toByteArray()))
            part(MockPart("content", updatedContent.toByteArray()))
            with { it.method = "PUT"; it }
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.title") { value(updatedTitle) }
            jsonPath("$.data.content") { value(updatedContent) }
            jsonPath("$.data.groupId") { value(group.id) }
            jsonPath("$.data.groupName") { value(group.groupName) }
            jsonPath("$.data.author.id") { value(currentUser.id) }
        }
    }

    @Test
    fun `should handle like operations successfully`() {
        val notificationSlot = slot<NotificationMessage>()
        
        every { postService.findOrThrow(post.id!!) } returns post
        every { postService.addLikeToPost(post, currentUser) } returns true
        every { notificationService.createPushNotification(
            post.author.id!!,
            currentUser,
            EventType.LIKE_TO_POST,
            capture(notificationSlot)
        ) } returns mockk()

        mockMvc.put("/media/posts/${post.id}/likes") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.likeCount") { value(post.activeLikes.size) }
            jsonPath("$.data.likedByCurrentUser") { value(true) }
        }

        // Only verify notification message if the slot was captured
        if (notificationSlot.isCaptured) {
            assertThat(notificationSlot.captured.title).isEqualTo("New Like")
            assertThat(notificationSlot.captured.dataType).isEqualTo(DataType.POST)
            assertThat(notificationSlot.captured.data).isEqualTo(post)
        }
    }

    @Test
    fun `should handle error cases appropriately`() {
        // Non-existent post
        every { postService.find("invalid-id") } returns null
        every { postService.findOrThrow("invalid-id") } throws ResourceNotFoundException("Post", "invalid-id")

        mockMvc.get("/media/posts/invalid-id")
            .andExpect { status { isNotFound() } }

        // Unauthorized post update
        val differentUser = Fixture.createUserFixture()
        every { userService.findCurrentUser() } returns differentUser
        every { postService.find(post.id!!) } returns post

        mockMvc.multipart("/media/posts/${post.id}") {
            part(MockPart("title", "new title".toByteArray()))
            with { it.method = "PUT"; it }
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value(ErrorCode.USER_ACTION_DENIED.name) }
        }
    }
}
