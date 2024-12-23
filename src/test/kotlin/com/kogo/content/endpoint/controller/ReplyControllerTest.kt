package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.`test-util`.Fixture
import com.kogo.content.common.*
import com.kogo.content.endpoint.common.*
import com.kogo.content.service.*
import com.kogo.content.storage.model.DataType
import com.kogo.content.storage.model.EventType
import com.kogo.content.storage.model.NotificationMessage
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ReplyControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean
    lateinit var replyService: ReplyService

    @MockkBean
    lateinit var commentService: CommentService

    @MockkBean
    lateinit var userService: UserService

    @MockkBean
    lateinit var notificationService: NotificationService

    private val user = Fixture.createUserFixture()
    private val topic = Fixture.createTopicFixture(user)
    private val post = Fixture.createPostFixture(group = topic, author = user)
    private val comment = Fixture.createCommentFixture(post = post, author = user)
    private val reply = Fixture.createReplyFixture(comment = comment, author = user)
    private val replyAggregate = Fixture.createReplyAggregateFixture(reply)

    private fun buildCommentApiUrl(vararg paths: String, params: Map<String, String> = emptyMap()): String {
        val baseUrl = "/media/comments"
        val url = if (paths.isNotEmpty()) "$baseUrl/${paths.joinToString("/")}" else baseUrl
        val paramBuilder = StringBuilder()
        params.forEach { paramBuilder.append("${it.key}=${it.value}&") }
        return if (paramBuilder.isNotEmpty()) "$url?$paramBuilder" else url
    }

    private fun buildReplyApiUrl(vararg paths: String, params: Map<String, String> = emptyMap()): String {
        val baseUrl = "/media/replies"
        val url = if (paths.isNotEmpty()) "$baseUrl/${paths.joinToString("/")}" else baseUrl
        val paramBuilder = StringBuilder()
        params.forEach { paramBuilder.append("${it.key}=${it.value}&") }
        return if (paramBuilder.isNotEmpty()) "$url?$paramBuilder" else url
    }

    @BeforeEach
    fun setup() {
        every { userService.findCurrentUser() } returns user
        every { commentService.find(comment.id!!) } returns comment
        every { replyService.find(reply.id!!) } returns reply
        every { replyService.findAggregate(reply.id!!) } returns replyAggregate
        every { replyService.hasUserLikedReply(reply, user) } returns false
    }

    @Test
    fun `should return replies with pagination metadata by comment id`() {
        val commentId = comment.id!!
        val replies = listOf(
            Fixture.createReplyFixture(comment = comment, author = user),
            Fixture.createReplyFixture(comment = comment, author = user)
        )
        val replyStats = replies.map { Fixture.createReplyAggregateFixture(it) }

        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken.create())
        val nextPageToken = paginationRequest.pageToken.nextPageToken(mapOf(
            "id" to CursorValue("next-token", CursorValueType.STRING)
        ))
        val paginationSlice = PaginationSlice(replyStats, nextPageToken)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { replyService.findReplyAggregatesByComment(comment, capture(paginationRequestSlot)) } returns paginationSlice
        replies.forEachIndexed { index, reply ->
            every { replyService.findAggregate(reply.id!!) } returns replyStats[index]
            every { replyService.hasUserLikedReply(reply, user) } returns false
        }

        mockMvc.get(buildCommentApiUrl(commentId, "replies", params = mapOf(
            "limit" to "${paginationRequest.limit}"
        )))
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.data.length()") { value(replies.size) }
                jsonPath("$.data[0].likeCount") { value(replyStats[0].likeCount) }
                jsonPath("$.data[0].likedByCurrentUser") { value(false) }
                header { string(PaginationSlice.HEADER_PAGE_TOKEN, nextPageToken.encode()) }
                header { string(PaginationSlice.HEADER_PAGE_SIZE, "${paginationRequest.limit}") }
            }

        val capturedRequest = paginationRequestSlot.captured
        assertThat(capturedRequest.limit).isEqualTo(paginationRequest.limit)
        assertThat(capturedRequest.pageToken.toString()).isEqualTo(paginationRequest.pageToken.toString())
    }

    @Test
    fun `should create a new reply to comment`() {
        val commentId = comment.id!!
        val newReply = Fixture.createReplyFixture(comment = comment, author = user)
        val newReplyStat = Fixture.createReplyAggregateFixture(newReply)

        // mock message
        val notificationMessageSlot = slot<NotificationMessage>()
        every { notificationService.createPushNotification(comment.author.id!!, user, EventType.CREATE_REPLY_TO_COMMENT, capture(notificationMessageSlot))} returns mockk()

        every { replyService.create(comment, user, any()) } returns newReply
        every { replyService.findAggregate(newReply.id!!) } returns newReplyStat
        every { replyService.hasUserLikedReply(newReply, user) } returns false

        mockMvc.perform(
            multipart(buildCommentApiUrl(commentId, "replies"))
                .part(MockPart("content", newReply.content.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(newReply.content))
            .andExpect(jsonPath("$.data.commentId").value(comment.id))
            .andExpect(jsonPath("$.data.likeCount").value(newReplyStat.likeCount))
            .andExpect(jsonPath("$.data.likedByCurrentUser").value(false))

        val capturedNotificationMessage = notificationMessageSlot.captured
        assertThat(capturedNotificationMessage.title).isEqualTo("New Reply")
        assertThat(capturedNotificationMessage.body).isEqualTo("${user.username} replied: ${newReply.content}")
        assertThat(capturedNotificationMessage.dataType).isEqualTo(DataType.REPLY)
        assertThat(capturedNotificationMessage.data).isEqualTo(newReply)
    }

    @Test
    fun `should update an existing reply if user is the author`() {
        val updatedReply = reply.copy()
        updatedReply.content = "Updated content"
        replyAggregate.reply.content = updatedReply.content

        every { replyService.isUserAuthor(reply, user) } returns true
        every { replyService.update(reply, any()) } returns updatedReply
        every { replyService.findAggregate(updatedReply.id!!) } returns replyAggregate
        every { replyService.hasUserLikedReply(updatedReply, user) } returns false

        mockMvc.perform(
            multipart(buildReplyApiUrl(reply.id!!))
                .part(MockPart("content", updatedReply.content.toByteArray()))
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(updatedReply.content))
    }

    @Test
    fun `should add like to reply`() {
        val replyId = reply.id!!

        every { replyService.hasUserLikedReply(reply, user) } returns false
        every { replyService.addLike(reply, user) } returns mockk()

        // mock message
        val notificationMessageSlot = slot<NotificationMessage>()
        every { notificationService.createPushNotification(comment.author.id!!, user, EventType.LIKE_TO_REPLY, capture(notificationMessageSlot))} returns mockk()

        mockMvc.put(buildReplyApiUrl(replyId, "likes")) {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(replyId) }
            jsonPath("$.data.likeCount") { value(replyAggregate.likeCount) }
            jsonPath("$.data.likedByCurrentUser") { value(false) }
            jsonPath("$.message") { value("User's like added successfully to reply $replyId") }
        }
        val capturedNotificationMessage = notificationMessageSlot.captured
        assertThat(capturedNotificationMessage.title).isEqualTo("New Like")
        assertThat(capturedNotificationMessage.body).isEqualTo("${user.id!!} liked your reply: ${reply.content}")
        assertThat(capturedNotificationMessage.dataType).isEqualTo(DataType.REPLY)
        assertThat(capturedNotificationMessage.data).isEqualTo(reply)
    }

    @Test
    fun `should return 403 when updating reply if user is not the author`() {
        every { replyService.isUserAuthor(reply, user) } returns false

        mockMvc.perform(
            multipart(buildReplyApiUrl(reply.id!!))
                .part(MockPart("content", "Updated content".toByteArray()))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value(ErrorCode.USER_ACTION_DENIED.name))
            .andExpect(jsonPath("$.details").value("user is not the author of the reply"))
    }

    @Test
    fun `should delete a reply if user is the author`() {
        every { replyService.isUserAuthor(reply, user) } returns true
        every { replyService.delete(reply) } returns Unit

        mockMvc.delete(buildReplyApiUrl(reply.id!!))
            .andExpect {
                status { isOk() }
            }

        verify { replyService.delete(reply) }
    }

    @Test
    fun `should return 403 when deleting reply if user is not the author`() {
        every { replyService.isUserAuthor(reply, user) } returns false

        mockMvc.delete(buildReplyApiUrl(reply.id!!))
            .andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value(ErrorCode.USER_ACTION_DENIED.name) }
                jsonPath("$.details") { value("user is not the author of the reply") }
            }
    }

    @Test
    fun `should return 400 when user has already liked the reply`() {
        every { replyService.hasUserLikedReply(reply, user) } returns true

        mockMvc.perform(
            multipart(buildReplyApiUrl(reply.id!!, "likes"))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value(ErrorCode.BAD_REQUEST.name))
            .andExpect(jsonPath("$.details").value("user has already liked this reply Id: ${reply.id}"))
    }

    @Test
    fun `should remove like from reply`() {
        every { replyService.hasUserLikedReply(reply, user) } returns true
        every { replyService.removeLike(reply, user) } returns Unit

        mockMvc.delete(buildReplyApiUrl(reply.id!!, "likes"))
            .andExpect {
                status { isOk() }
                jsonPath("$.message") { value("User's like removed successfully to reply ${reply.id}") }
            }
    }

    @Test
    fun `should return 400 when removing like from reply user hasn't liked`() {
        every { replyService.hasUserLikedReply(reply, user) } returns false

        mockMvc.delete(buildReplyApiUrl(reply.id!!, "likes"))
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value(ErrorCode.BAD_REQUEST.name) }
                jsonPath("$.details") { value("user didn't put a like on this reply Id: ${reply.id}") }
            }
    }

    @Test
    fun `should return 404 when adding like to non-existing reply`() {
        val invalidReplyId = "invalid-reply-id"
        every { replyService.find(invalidReplyId) } returns null

        mockMvc.perform(
            multipart(buildReplyApiUrl(invalidReplyId, "likes"))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when removing like from non-existing reply`() {
        val invalidReplyId = "invalid-reply-id"
        every { replyService.find(invalidReplyId) } returns null

        mockMvc.delete(buildReplyApiUrl(invalidReplyId, "likes"))
            .andExpect {
                status { isNotFound() }
            }
    }
}

