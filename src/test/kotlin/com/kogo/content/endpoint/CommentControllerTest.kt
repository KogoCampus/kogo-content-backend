package com.kogo.content.endpoint

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationResponse
import com.kogo.content.service.entity.CommentService
import com.kogo.content.service.entity.PostService
import com.kogo.content.service.entity.TopicService
import com.kogo.content.service.entity.UserContextService
import com.kogo.content.service.pagination.PageToken
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.PostRepository
import com.kogo.content.util.Fixture
import com.ninjasquad.springmockk.MockkBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.junit.jupiter.api.Test
import com.kogo.content.util.fixture
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean
    lateinit var topicService: TopicService

    @MockkBean
    lateinit var commentService: CommentService

    @MockkBean
    lateinit var postService: PostService

    @MockkBean
    lateinit var userService: UserContextService

    @MockkBean
    lateinit var postRepository: PostRepository

    @MockkBean
    lateinit var commentRepository: CommentRepository

    @BeforeEach
    fun setUp() {
        every { postRepository.findByIdOrNull(any()) } returns Fixture.createPostFixture(Fixture.createTopicFixture()).apply {
            createdAt = Instant.now()
        }

        every { commentRepository.findByIdOrNull(any()) } returns Fixture.createCommentFixture(Fixture.createPostFixture(Fixture.createTopicFixture())).apply {
            createdAt = Instant.now()
        }
    }

    private fun buildCommentApiUrl(topicId: String, postId: String, vararg paths: String): String {
        val baseUrl = "/media/topics/$topicId/posts/$postId/comments"

        return if (paths.isNotEmpty()) "$baseUrl/" + paths.joinToString("/") else baseUrl
    }

    private fun buildReplyApiUrl(topicId: String, postId: String, commentId: String, vararg paths: String): String {
        val baseUrl = "/media/topics/$topicId/posts/$postId/comments/$commentId"
        return if (paths.isNotEmpty()) "$baseUrl/" + paths.joinToString("/") else baseUrl
    }

    @Test
    fun `should return a list of comments under post id`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val comments = listOf(Fixture.createCommentFixture(post), Fixture.createCommentFixture(post))
        val postId = post.id!!
        val paginationRequest = PaginationRequest(limit = 10, pageToken = PageToken())
        val paginationResponse = PaginationResponse(comments, paginationRequest.pageToken.nextPageToken("sample-next-page-token"))
        val paginationRequestSlot = slot<PaginationRequest>()

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { commentService.listCommentsByPost(post, capture(paginationRequestSlot)) } returns paginationResponse

        mockMvc.get(buildCommentApiUrl(topic.id!!, postId)) {
            param("limit", paginationRequest.limit.toString())
        }
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.length()") { value(comments.size) } }
            .andExpect { header { string("next_page", "sample-next-page-token") } }
            .andExpect { header { string("item_count", "${comments.size}") } }

        val capturedPaginationRequest = paginationRequestSlot.captured
        assertThat(capturedPaginationRequest.limit).isEqualTo(paginationRequest.limit)
        assertThat(capturedPaginationRequest.pageToken).isEqualTo(paginationRequest.pageToken)
    }

    @Test
    fun `should return 404 when comment is not found`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val postId = post.id!!
        val commentId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { commentService.findComment(commentId) } returns null
        mockMvc.get(buildReplyApiUrl(topic.id!!, postId, commentId))
            .andExpect { status { isNotFound() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
    }

    @Test
    fun `should create a new comment`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val postId = post.id!!
        val user = Fixture.createUserFixture()
        val comment = Fixture.createCommentFixture(post)
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.createComment(post, user, any()) } returns comment
        every { postRepository.findByIdOrNull(any()) } returns post.apply { createdAt = Instant.now() }
        mockMvc.perform(
            multipart(buildCommentApiUrl(topic.id!!, postId))
                .part(MockPart("content", comment.content.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with{ it.method = "POST"; it })
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(comment.content))
            .andExpect(jsonPath("$.data.postId").value(post.id))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }

    @Test
    fun `should return 404 if post is not found`() {
        val topic = Fixture.createTopicFixture()
        val postId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns null
        mockMvc.perform(
            multipart(buildCommentApiUrl(topic.id!!, postId))
                .part(MockPart("content", "testing".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should return 404 if comment is not found`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val commentId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.findComment(commentId) } returns null
        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, commentId, "replies"))
                .part(MockPart("content", "testing".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should delete an existing comment if user is the owner`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val comment = Fixture.createCommentFixture(post)
        val currentUser = Fixture.createUserFixture()

        // Mocking repository and service responses
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.findComment(comment.id!!) } returns comment
        every { commentService.isCommentAuthor(comment, currentUser) } returns true
        every { commentService.deleteComment(comment) } returns Unit

        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!))
            .andExpect { status { isOk() } }
    }

    @Test
    fun `should return 403 if user is not the comment owner`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val comment = Fixture.createCommentFixture(post)
        val currentUser = Fixture.createUserFixture() // The current user (not the owner)
        val differentUser = Fixture.createUserFixture() // Another user to simulate the owner

        // Mocking repository and service responses
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.findComment(comment.id!!) } returns comment
        every { commentService.isCommentAuthor(comment, currentUser) } returns false // User is not the owner
        every { userService.getCurrentUserDetails() } returns currentUser

        // Perform delete request and expect 403 Forbidden
        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!))
            .andExpect { status { isForbidden() } }
            .andExpect { jsonPath("$.error.reason").value("USER_IS_NOT_OWNER") }
    }

    @Test
    fun `should return 404 if deleting the non-existing comment`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val commentId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.findComment(commentId) } returns null
        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, commentId))
            .andExpect { status().isNotFound }
    }

    @Test
    fun `should update an existing comment if user is the owner`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val comment = Fixture.createCommentFixture(post)
        val currentUser = Fixture.createUserFixture() // Simulate the owner of the comment
        val updatedContent = "new content"
        val updatedAt = Instant.now()
        val commentUpdate = CommentUpdate(content = updatedContent)

        val newComment = comment.copy(content = updatedContent, updatedAt = updatedAt)

        // Mocking repository and service responses
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.findComment(comment.id!!) } returns comment
        every { commentService.isCommentAuthor(comment, currentUser) } returns true // User is the owner
        every { commentService.updateComment(comment, commentUpdate) } returns newComment
        every { userService.getCurrentUserDetails() } returns currentUser

        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!))
                .part(MockPart("content", newComment.content.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(newComment.content))
            .andExpect(jsonPath("$.data.postId").value(comment.post.id))
            .andExpect(jsonPath("$.data.createdAt").value(comment.createdAt.toString())) // Ensure createdAt remains unchanged
            .andExpect(jsonPath("$.data.updatedAt").value(updatedAt.toString()))
    }

    @Test
    fun `should return 403 if user is not the comment owner during update`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val comment = Fixture.createCommentFixture(post)
        val currentUser = Fixture.createUserFixture() // Simulate the current user
        val updatedContent = "new content"
        val commentUpdate = CommentUpdate(content = updatedContent)

        // Mocking repository and service responses
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.findComment(comment.id!!) } returns comment
        every { commentService.isCommentAuthor(comment, currentUser) } returns false // User is not the owner
        every { userService.getCurrentUserDetails() } returns currentUser // Mock the user as non-owner

        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!))
                .part(MockPart("content", updatedContent.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value("USER_IS_NOT_OWNER"))
            .andExpect(jsonPath("$.details").value("You are not the owner of Comment with id: ${comment.id}."))
    }

    @Test
    fun `should return 404 if updating non-existing comment`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val commentId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.findComment(commentId) } returns null
        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, commentId))
                .part(MockPart("content", "testing".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should create a like under the comment`() {
        val topic = Fixture.createTopicFixture()
        val post = Fixture.createPostFixture(topic)
        val comment = Fixture.createCommentFixture(post)
        val user = Fixture.createUserFixture()

        val topicId = topic.id!!
        val postId = post.id!!
        val commentId = comment.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.findComment(comment.id!!) } returns comment
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.hasUserLikedComment(comment, user)} returns false
        every { commentService.addLikeToComment(comment, user) } returns mockk()

        mockMvc.perform(
            multipart("/media/topics/$topicId/posts/$postId/comments/$commentId/likes")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.error.reason").doesNotExist())
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.id").value(commentId))
    }

    @Test
    fun `should return 404 if creating a like under non-existing comment`() {
        val topic = Fixture.createTopicFixture()
        val topicId = topic.id!!
        val post = Fixture.createPostFixture(Fixture.createTopicFixture())
        val postId = post.id!!
        val commentId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.findComment(commentId) } returns null
        mockMvc.perform(
            multipart("/media/topics/$topicId/posts/$postId/comments/$commentId/likes")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should return 400 when creating a duplicate like under the same comment`() {
        val topic = Fixture.createTopicFixture()
        val topicId = topic.id!!
        val post = Fixture.createPostFixture(Fixture.createTopicFixture())
        val postId = post.id!!
        val user = Fixture.createUserFixture()
        val comment = Fixture.createCommentFixture(post)
        val commentId = comment.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { commentService.findComment(comment.id!!) } returns comment
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.hasUserLikedComment(comment, user) } returns mockk()

        mockMvc.perform(
            multipart("/media/topics/$topicId/posts/$postId/comments/$commentId/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .with{ it.method = "POST"; it }
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value(ErrorCode.BAD_REQUEST.name))
            .andExpect(jsonPath("$.details").value("user already liked this comment $commentId."))
    }

    @Test
    fun `should remove a like from the comment`() {
        val topic = Fixture.createTopicFixture()
        val topicId = topic.id!!
        val post = Fixture.createPostFixture(Fixture.createTopicFixture())
        val postId = post.id!!
        val user = Fixture.createUserFixture()
        val comment = Fixture.createCommentFixture(post)
        val commentId = comment.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.hasUserLikedComment(comment, user) } returns mockk()
        mockMvc.delete("/media/topics/$topicId/posts/$postId/comments/$commentId/likes")
            .andExpect { status().isOk }
            .andExpect { jsonPath("$.message").value("User's like removed successfully to comment $commentId.") }
    }

    @Test
    fun `should return 404 when removing a like under non-existing comment`() {
        val topic = Fixture.createTopicFixture()
        val topicId = topic.id!!
        val post = Fixture.createPostFixture(Fixture.createTopicFixture())
        val commentId = "invalid"
        val postId = post.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.findComment(commentId) } returns null

        mockMvc.delete("/media/topics/$topicId/posts/$postId/comments/$commentId/likes")
            .andExpect { status().isNotFound }

    }

    @Test
    fun `should return 400 if removing a non-existing like`() {
        val topic = Fixture.createTopicFixture()
        val topicId = topic.id!!
        val post = Fixture.createPostFixture(Fixture.createTopicFixture())
        val postId = post.id!!
        val user = Fixture.createUserFixture()
        val comment = Fixture.createCommentFixture(post)
        val commentId = comment.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.findComment(comment.id!!) } returns comment
        every { commentService.hasUserLikedComment(comment, user) } returns false

        mockMvc.delete("/media/topics/$topicId/posts/$postId/likes")
            .andExpect { status().isBadRequest }
            .andExpect { jsonPath("$.message").value("user haven't liked this comment $commentId.") }
    }
}
