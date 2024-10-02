package com.kogo.content.endpoint

import com.kogo.content.service.CommentService
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.service.UserContextService
import com.kogo.content.storage.entity.*
import com.ninjasquad.springmockk.MockkBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.junit.jupiter.api.Test
import com.kogo.content.util.fixture
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

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
            "author" to createUserFixture(),
            "comments" to emptyList<Any>(),
            "attachments" to emptyList<Any>(),
        )
    }

    private fun createCommentFixture(post: Post) = fixture<Comment> {
        mapOf(
            "parentId" to post.id,
            "author" to createUserFixture(),
        )
    }

    private fun createReplyFixture(comment: Comment) = fixture<Comment> {
        mapOf(
            "parentId" to comment.id,
            "author" to createUserFixture(),
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
        mockMvc.perform(
            multipart(buildCommentApiUrl(topic.id!!, postId))
                .part(MockPart("content", comment.content.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with{ it.method = "POST"; it })
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(comment.content))
            .andExpect(jsonPath("$.data.parentId").value(post.id))
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
        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, postId, parentComment.id!!, "replies"))
                .part(MockPart("content", reply.content.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with{ it.method = "POST"; it })
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(reply.content))
            .andExpect(jsonPath("$.data.parentId").value(parentComment.id))
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
    fun `should delete an existing comment`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val comment = createCommentFixture(post)

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(comment.id!!) } returns comment
        every { commentService.delete(comment.id!!) } returns Unit
        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!))
            .andExpect { status { isOk() } }
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
    fun `should update an existing comment`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val comment = createCommentFixture(post)
        val newComment = Comment(
            id = comment.id,
            content = "new content",
            author = comment.author,
            parentType = comment.parentType,
            parentId = comment.parentId,
        )

        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(comment.id!!) } returns comment
        every { commentService.update(comment, any()) } returns newComment
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
            .andExpect(jsonPath("$.message").value("User's like added successfully to comment $commentId."))
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
