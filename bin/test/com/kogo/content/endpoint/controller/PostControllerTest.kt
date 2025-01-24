package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.service.*
import com.kogo.test.util.Fixture
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.storage.model.*
import com.kogo.content.storage.model.entity.*
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.*
import org.assertj.core.api.Assertions.assertThat

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean private lateinit var postService: PostService
    @MockkBean private lateinit var groupService: GroupService
    @MockkBean private lateinit var userService: UserService
    @MockkBean private lateinit var pushNotificationService: PushNotificationService

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
            jsonPath("$.data.group.id") { value(group.id) }
            jsonPath("$.data.author.id") { value(currentUser.id) }
        }

        verify { postService.addViewer(post, currentUser) }
    }

    @Test
    fun `should create post successfully when user follows group`() {
        group.followers.add(Follower(currentUser))
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
            jsonPath("$.data.group.id") { value(group.id) }
            jsonPath("$.data.author.id") { value(currentUser.id) }
        }
    }

    @Test
    fun `should fail to create post when user does not follow group`() {
        val unfollowedGroup = Fixture.createGroupFixture(owner = currentUser).apply {
            followers = mutableListOf()
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
            jsonPath("$.data.group.id") { value(group.id) }
            jsonPath("$.data.author.id") { value(currentUser.id) }
        }
    }

    @Test
    fun `should handle like operations successfully`() {
        val expectedNotification = Notification(
            recipient = post.author,
            sender = currentUser,
            title = post.title.take(50) + if (post.title.length > 50) "..." else "",
            body = "${currentUser.username} liked your post"
        )

        every { postService.findOrThrow(post.id!!) } returns post
        every { postService.addLikeToPost(post, currentUser) } returns true

        mockMvc.put("/media/posts/${post.id}/likes") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.likeCount") { value(post.activeLikes.size) }
            jsonPath("$.data.likedByCurrentUser") { value(true) }
        }

        verify(exactly = 1) {
            postService.findOrThrow(post.id!!)
            postService.addLikeToPost(post, currentUser)
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
