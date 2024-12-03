package com.kogo.content.endpoint.controller

import com.kogo.content.common.PaginationRequest
import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.`test-util`.Fixture
import com.kogo.content.service.*
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.UserRepository
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
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean
    private lateinit var userRepository: UserRepository

    @MockkBean
    lateinit var commentService: CommentService

    @MockkBean
    lateinit var postService: PostService

    @MockkBean
    lateinit var topicService: TopicService

    @MockkBean
    lateinit var replyService: ReplyService

    @MockkBean
    lateinit var userService: UserService

    @MockkBean
    lateinit var notificationService: NotificationService

    private val user = Fixture.createUserFixture()
    private val topic = Fixture.createTopicFixture(user)
    private val topicAggregate = Fixture.createTopicAggregateFixture(topic)
    private val post = Fixture.createPostFixture(topic = topic, author = user)
    private val postAggregate = Fixture.createPostAggregateFixture(post)
    private val comment = Fixture.createCommentFixture(post = post, author = user)
    private val commentAggregate = Fixture.createCommentAggregateFixture(comment)

    private fun buildCommentApiUrl(vararg paths: String, params: Map<String, String> = emptyMap()): String {
        val baseUrl = "/media/comments"
        val url = if (paths.isNotEmpty()) "$baseUrl/${paths.joinToString("/")}" else baseUrl
        val paramBuilder = StringBuilder()
        params.forEach { paramBuilder.append("${it.key}=${it.value}&") }
        return if (paramBuilder.isNotEmpty()) "$url?$paramBuilder" else url
    }

    @BeforeEach
    fun setup() {
        every { userService.getCurrentUser() } returns user
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(comment.id!!) } returns comment
        every { commentService.findAggregate(comment.id!!) } returns commentAggregate
        every { commentService.hasUserLikedComment(comment.id!!, user) } returns false
    }

    @Test
    fun `should return a single comment`() {
        mockMvc.get(buildCommentApiUrl(comment.id!!))
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.data.id") { value(comment.id) }
                jsonPath("$.data.content") { value(comment.content) }
                jsonPath("$.data.likeCount") { value(commentAggregate.likeCount) }
                jsonPath("$.data.replyCount") { value(commentAggregate.replyCount) }
            }
    }

    @Test
    fun `should return 404 when comment is not found`() {
        val invalidCommentId = "invalid-comment-id"
        every { commentService.find(invalidCommentId) } returns null

        mockMvc.get(buildCommentApiUrl(invalidCommentId))
            .andExpect {
                status { isNotFound() }
                content { contentType(MediaType.APPLICATION_JSON) }
            }
    }

    @Test
    fun `should update an existing comment if user is the author`() {
        val updatedComment = comment.copy(content = "Updated content")
        val updatedAggregate = commentAggregate.copy(comment = updatedComment)

        every { commentService.isUserAuthor(comment, user) } returns true
        every { commentService.update(comment, any()) } returns updatedComment
        every { commentService.findAggregate(updatedComment.id!!) } returns updatedAggregate

        mockMvc.perform(
            multipart(buildCommentApiUrl(comment.id!!))
                .part(MockPart("content", updatedComment.content.toByteArray()))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(updatedComment.content))
    }

    @Test
    fun `should return 403 when updating comment if user is not the author`() {
        val differentUser = Fixture.createUserFixture()
        every { userService.getCurrentUser() } returns differentUser
        every { commentService.isUserAuthor(comment, differentUser) } returns false

        mockMvc.perform(
            multipart(buildCommentApiUrl(comment.id!!))
                .part(MockPart("content", "Updated content".toByteArray()))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value(ErrorCode.USER_ACTION_DENIED.name))
            .andExpect(jsonPath("$.details").value("user is not the author of the comment"))
    }

    @Test
    fun `should delete a comment if user is the author`() {
        every { commentService.isUserAuthor(comment, user) } returns true
        every { commentService.delete(comment) } returns Unit

        mockMvc.delete(buildCommentApiUrl(comment.id!!))
            .andExpect {
                status { isOk() }
            }

        verify { commentService.delete(comment) }
    }

    @Test
    fun `should return 403 when deleting comment if user is not the author`() {
        every { commentService.isUserAuthor(comment, user) } returns false

        mockMvc.delete(buildCommentApiUrl(comment.id!!))
            .andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value(ErrorCode.USER_ACTION_DENIED.name) }
                jsonPath("$.details") { value("user is not the author of the comment") }
            }
    }

    @Test
    fun `should add like to comment`() {
        every { commentService.hasUserLikedComment(comment.id!!, user) } returns false
        every { commentService.addLike(comment, user) } returns mockk()

        // mock message
        val notificationMessageSlot = slot<NotificationMessage>()
        every { notificationService.createPushNotification(comment.author.id!!, user, EventType.LIKE_TO_COMMENT, capture(notificationMessageSlot))} returns mockk()

        mockMvc.perform(
            multipart(buildCommentApiUrl(comment.id!!, "likes"))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("User's like added successfully to comment ${comment.id}"))
        val capturedNotificationMessage = notificationMessageSlot.captured
        assertThat(capturedNotificationMessage.title).isEqualTo("New Like")
        assertThat(capturedNotificationMessage.body).isEqualTo("${user.username} liked your comment: ${comment.content}")
        assertThat(capturedNotificationMessage.dataType).isEqualTo(DataType.COMMENT)
        assertThat(capturedNotificationMessage.data).isEqualTo(comment)
    }

    @Test
    fun `should return 400 when user has already liked the comment`() {
        every { commentService.hasUserLikedComment(comment.id!!, user) } returns true

        mockMvc.perform(
            multipart(buildCommentApiUrl(comment.id!!, "likes"))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.details").value("user has already liked this comment Id: ${comment.id}"))
    }

    @Test
    fun `should remove like from comment`() {
        every { commentService.hasUserLikedComment(comment.id!!, user) } returns true
        every { commentService.removeLike(comment, user) } returns Unit

        mockMvc.delete(buildCommentApiUrl(comment.id!!, "likes"))
            .andExpect {
                status { isOk() }
                jsonPath("$.message") { value("User's like removed successfully to comment ${comment.id}.") }
            }
    }

    @Test
    fun `should return 400 when removing like from comment user hasn't liked`() {
        every { commentService.hasUserLikedComment(comment.id!!, user) } returns false

        mockMvc.delete(buildCommentApiUrl(comment.id!!, "likes"))
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value(ErrorCode.BAD_REQUEST.name) }
                jsonPath("$.details") { value("user didn't put a like on this comment Id: ${comment.id}.") }
            }
    }

    @Test
    fun `should return 404 when adding like to non-existing comment`() {
        val invalidCommentId = "invalid-comment-id"
        every { commentService.find(invalidCommentId) } returns null

        mockMvc.perform(
            multipart(buildCommentApiUrl(invalidCommentId, "likes"))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when removing like from non-existing comment`() {
        val invalidCommentId = "invalid-comment-id"
        every { commentService.find(invalidCommentId) } returns null

        mockMvc.delete(buildCommentApiUrl(invalidCommentId, "likes"))
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `should return 404 when creating reply to non-existing comment`() {
        val invalidCommentId = "invalid-comment-id"
        every { commentService.find(invalidCommentId) } returns null

        mockMvc.perform(
            multipart(buildCommentApiUrl(invalidCommentId, "replies"))
                .part(MockPart("content", "Test content".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isNotFound)
    }
}
