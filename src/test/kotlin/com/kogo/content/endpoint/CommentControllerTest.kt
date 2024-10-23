package com.kogo.content.endpoint

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.searchengine.SearchIndexService
import com.kogo.content.service.CommentService
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.service.UserContextService
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.PostRepository
import com.ninjasquad.springmockk.MockkBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.junit.jupiter.api.Test
import com.kogo.content.util.fixture
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
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
    lateinit var searchIndexService: SearchIndexService

    @MockkBean
    lateinit var postRepository: PostRepository

    @MockkBean
    lateinit var commentRepository: CommentRepository

    @BeforeEach
    fun setUp() {
        every { postRepository.findByIdOrNull(any()) } returns createPostFixture(createTopicFixture()).apply {
            createdAt = Instant.now()
        }

        every { commentRepository.findByIdOrNull(any()) } returns createCommentFixture(createPostFixture(createTopicFixture())).apply {
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

    private fun createTopicFixture() = fixture<Topic> {
        mapOf(
            "owner" to createUserFixture()
        )
    }

    private fun createPostFixture(topic: Topic) = fixture<Post> {
        mapOf(
            "topic" to topic,
            "owner" to createUserFixture(),
            "comments" to emptyList<Any>(),
            "attachments" to emptyList<Any>(),
        )
    }

    private fun createCommentFixture(post: Post) = fixture<Comment> {
        mapOf(
            "parentId" to post.id,
            "owner" to createUserFixture(),
            "createdAt" to Instant.now()
        )
    }

    private fun createReplyFixture(comment: Comment) = fixture<Comment> {
        mapOf(
            "parentId" to comment.id,
            "owner" to createUserFixture(),
            "createdAt" to Instant.now()
        )
    }

    private fun createUserFixture() = fixture<UserDetails>()



    @Test
    fun `should return a list of comments under post id`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val comments = listOf(createCommentFixture(post), createCommentFixture(post))
        val postId = post.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { commentService.findCommentsByParentId(postId) } returns comments
        mockMvc.get(buildCommentApiUrl(topic.id!!, postId))
            .andExpect { status { isOk() }}
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.length()") {value(comments.size)} }
            .andExpect { jsonPath("$.data[0].createdAt") { exists() } }
    }

    @Test
    fun `should return a list of replies under comment id`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val parentComment = createCommentFixture(post)
        val comments = listOf(createReplyFixture(parentComment), createReplyFixture(parentComment))
        val parentCommentId = parentComment.id!!
        val postId = post.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { commentService.find(parentCommentId) } returns parentComment
        every { commentService.findCommentsByParentId(parentCommentId) } returns comments
        mockMvc.get(buildReplyApiUrl(topic.id!!, postId, parentCommentId, "replies"))
            .andExpect { status { isOk() }}
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.length()") {value(comments.size)} }
            .andExpect { jsonPath("$.data[0].createdAt") { exists() } }
    }

    @Test
    fun `should return 404 when comment is not found`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val postId = post.id!!
        val commentId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { commentService.find(commentId) } returns null
        mockMvc.get(buildReplyApiUrl(topic.id!!, postId, commentId))
            .andExpect { status { isNotFound() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
    }

    @Test
    fun `should create a new comment`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val postId = post.id!!
        val user = createUserFixture()
        val comment = createCommentFixture(post)
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.create(postId, CommentParentType.POST, user, any()) } returns comment
        every { postRepository.findByIdOrNull(any()) } returns post.apply { createdAt = Instant.now() }
        every { searchIndexService.addDocument(any(), any()) } returns Unit
        mockMvc.perform(
            multipart(buildCommentApiUrl(topic.id!!, postId))
                .part(MockPart("content", comment.content.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with{ it.method = "POST"; it })
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(comment.content))
            .andExpect(jsonPath("$.data.parentId").value(post.id))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }

    @Test
    fun `should create a new reply`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val postId = post.id!!
        val user = createUserFixture()
        val parentComment = createCommentFixture(post)
        val reply = createReplyFixture(parentComment)

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { commentService.find(parentComment.id!!)} returns parentComment
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.create(parentComment.id!!, CommentParentType.COMMENT, user, any()) } returns reply
        every { postRepository.findByIdOrNull(any()) } returns post.apply { createdAt = Instant.now() }
        every { searchIndexService.addDocument(any(), any()) } returns Unit
        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, postId, parentComment.id!!, "replies"))
                .part(MockPart("content", reply.content.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with{ it.method = "POST"; it })
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(reply.content))
            .andExpect(jsonPath("$.data.parentId").value(parentComment.id))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }

    @Test
    fun `should return 404 if post is not found`() {
        val topic = createTopicFixture()
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
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val commentId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(commentId) } returns null
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
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val comment = createCommentFixture(post)
        val reply1 = createReplyFixture(comment)
        val reply2 = createReplyFixture(comment)
        val replies = listOf(reply1, reply2)
        val currentUser = createUserFixture()

        // Mocking repository and service responses
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(comment.id!!) } returns comment
        every { commentService.isCommentOwner(comment, currentUser) } returns true
        every { commentService.delete(comment) } returns Unit
        every { searchIndexService.deleteDocument(any(), any()) } returns Unit

        // Mocking recursive replies deletion
        every { commentRepository.findAllById(comment.replies) } returns replies
        every { commentRepository.findAllById(reply1.replies) } returns emptyList() // No further replies
        every { commentRepository.findAllById(reply2.replies) } returns emptyList() // No further replies
        every { commentRepository.deleteById(any()) } just Runs
        every { userService.getCurrentUserDetails() } returns currentUser

        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!))
            .andExpect { status { isOk() } }
    }

    @Test
    fun `should return 403 if user is not the comment owner`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val comment = createCommentFixture(post)
        val currentUser = createUserFixture() // The current user (not the owner)
        val differentUser = createUserFixture() // Another user to simulate the owner

        // Mocking repository and service responses
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(comment.id!!) } returns comment
        every { commentService.isCommentOwner(comment, currentUser) } returns false // User is not the owner
        every { userService.getCurrentUserDetails() } returns currentUser

        // Perform delete request and expect 403 Forbidden
        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!))
            .andExpect { status { isForbidden() } }
            .andExpect { jsonPath("$.error.reason").value("USER_IS_NOT_OWNER") }
    }

    @Test
    fun `should return 404 if deleting the non-existing comment`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val commentId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(commentId) } returns null
        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, commentId))
            .andExpect { status().isNotFound }
    }

    @Test
    fun `should update an existing comment if user is the owner`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val comment = createCommentFixture(post)
        val currentUser = createUserFixture() // Simulate the owner of the comment
        val updatedContent = "new content"
        val commentUpdate = CommentUpdate(content = updatedContent)
        val newComment = comment.copy(content = updatedContent)

        // Mocking repository and service responses
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(comment.id!!) } returns comment
        every { commentService.isCommentOwner(comment, currentUser) } returns true // User is the owner
        every { commentService.update(comment, commentUpdate) } returns newComment
        every { searchIndexService.updateDocument(any(), any()) } returns Unit
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
            .andExpect(jsonPath("$.data.parentId").value(comment.parentId))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }

    @Test
    fun `should return 403 if user is not the comment owner during update`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val comment = createCommentFixture(post)
        val currentUser = createUserFixture() // Simulate the current user
        val updatedContent = "new content"
        val commentUpdate = CommentUpdate(content = updatedContent)

        // Mocking repository and service responses
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(comment.id!!) } returns comment
        every { commentService.isCommentOwner(comment, currentUser) } returns false // User is not the owner
        every { userService.getCurrentUserDetails() } returns currentUser // Mock the user as non-owner

        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!))
                .part(MockPart("content", updatedContent.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.reason").value("USER_IS_NOT_OWNER"))
    }

    @Test
    fun `should return 404 if updating non-existing comment`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val commentId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(commentId) } returns null
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
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val comment = createCommentFixture(post)
        val user = createUserFixture()

        val topicId = topic.id!!
        val postId = post.id!!
        val commentId = comment.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(comment.id!!) } returns comment
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.findLikeByUserIdAndParentId(user.id!!, comment.id!!)} returns null
        every { commentService.addLike(comment.id!!, user) } returns Unit

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
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val post = createPostFixture(createTopicFixture())
        val postId = post.id!!
        val commentId = "invalid"

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(commentId) } returns null
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
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val post = createPostFixture(createTopicFixture())
        val postId = post.id!!
        val user = createUserFixture()
        val comment = createCommentFixture(post)
        val commentId = comment.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { commentService.find(comment.id!!) } returns comment
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.findLikeByUserIdAndParentId(user.id!!, commentId) } returns mockk()

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
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val post = createPostFixture(createTopicFixture())
        val postId = post.id!!
        val user = createUserFixture()
        val comment = createCommentFixture(post)
        val commentId = comment.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.findLikeByUserIdAndParentId(user.id!!, postId) } returns mockk()
        mockMvc.delete("/media/topics/$topicId/posts/$postId/comments/$commentId/likes")
            .andExpect { status().isOk }
            .andExpect { jsonPath("$.message").value("User's like removed successfully to comment $commentId.") }
    }

    @Test
    fun `should return 404 when removing a like under non-existing comment`() {
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val post = createPostFixture(createTopicFixture())
        val commentId = "invalid"
        val postId = post.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(commentId) } returns null

        mockMvc.delete("/media/topics/$topicId/posts/$postId/comments/$commentId/likes")
            .andExpect { status().isNotFound }

    }

    @Test
    fun `should return 400 if removing a non-existing like`() {
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val post = createPostFixture(createTopicFixture())
        val postId = post.id!!
        val user = createUserFixture()
        val comment = createCommentFixture(post)
        val commentId = comment.id!!

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { commentService.find(comment.id!!) } returns comment
        every { commentService.findLikeByUserIdAndParentId(user.id!!, postId) } returns null

        mockMvc.delete("/media/topics/$topicId/posts/$postId/likes")
            .andExpect { status().isBadRequest }
            .andExpect { jsonPath("$.message").value("user haven't liked this comment $commentId.") }
    }
}
