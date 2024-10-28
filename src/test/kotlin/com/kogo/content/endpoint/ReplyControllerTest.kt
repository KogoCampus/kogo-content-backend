package com.kogo.content.endpoint

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.service.entity.*
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationResponse
import com.kogo.content.service.pagination.PageToken
import com.kogo.content.endpoint.`test-util`.Fixture
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
    lateinit var postService: PostService

    @MockkBean
    lateinit var topicService: TopicService

    @MockkBean
    lateinit var userService: UserContextService

    private val user = Fixture.createUserFixture()
    private val topic = Fixture.createTopicFixture(user)
    private val post = Fixture.createPostFixture(topic = topic, author = user)
    private val comment = Fixture.createCommentFixture(post = post, author = user)
    private val reply = Fixture.createReplyFixture(comment = comment, author = user)

    private fun buildReplyApiUrl(
        topicId: String,
        postId: String,
        commentId: String,
        vararg paths: String,
        params: Map<String, String> = emptyMap()
    ): String {
        val baseUrl = "/media/topics/$topicId/posts/$postId/comments/$commentId/replies"
        val url = if (paths.isNotEmpty()) "$baseUrl/${paths.joinToString("/")}" else baseUrl
        val paramBuilder = StringBuilder()
        params.forEach { paramBuilder.append("${it.key}=${it.value}&") }
        return if (paramBuilder.isNotEmpty()) "$url?$paramBuilder" else url
    }

    @BeforeEach
    fun setup() {
        every { userService.getCurrentUserDetails() } returns user
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post
        every { commentService.find(comment.id!!) } returns comment
        every { replyService.find(reply.id!!) } returns reply
    }

    @Test
    fun `should return replies with pagination metadata by comment id`() {
        val replies = listOf(
            Fixture.createReplyFixture(comment = comment, author = user),
            Fixture.createReplyFixture(comment = comment, author = user)
        )

        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken())
        val nextPageToken = paginationRequest.pageToken.nextPageToken("next-token")
        val paginationResponse = PaginationResponse(replies, nextPageToken)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { replyService.listRepliesByComment(comment, capture(paginationRequestSlot)) } returns paginationResponse

        mockMvc.get(buildReplyApiUrl(
            topicId = topic.id!!,
            postId = post.id!!,
            commentId = comment.id!!,
            params = mapOf("limit" to "${paginationRequest.limit}")
        ))
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.data.length()") { value(replies.size) }
                header { string(PaginationResponse.HEADER_NAME_PAGE_TOKEN, nextPageToken.toString()) }
                header { string(PaginationResponse.HEADER_NAME_PAGE_SIZE, "${paginationRequest.limit}") }
            }

        val capturedRequest = paginationRequestSlot.captured
        assertThat(capturedRequest.limit).isEqualTo(paginationRequest.limit)
        assertThat(capturedRequest.pageToken.toString()).isEqualTo(paginationRequest.pageToken.toString())
    }

    @Test
    fun `should create a new reply`() {
        val newReply = reply.copy()
        every { replyService.create(comment, user, any()) } returns newReply

        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!))
                .part(MockPart("content", newReply.content.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(newReply.content))
    }

    @Test
    fun `should return 404 when creating reply for non-existing comment`() {
        val invalidCommentId = "invalid-comment-id"
        every { commentService.find(invalidCommentId) } returns null

        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, invalidCommentId))
                .part(MockPart("content", "Test content".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should update an existing reply if user is the author`() {
        val updatedReply = reply.copy()
        updatedReply.content = "Updated content"

        every { replyService.isUserAuthor(reply, user) } returns true
        every { replyService.update(reply, any()) } returns updatedReply

        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!, reply.id!!))
                .part(MockPart("content", updatedReply.content.toByteArray()))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").value(updatedReply.content))
    }

    @Test
    fun `should return 403 when updating reply if user is not the author`() {
        every { replyService.isUserAuthor(reply, user) } returns false

        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!, reply.id!!))
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

        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!, reply.id!!))
            .andExpect {
                status { isOk() }
            }

        verify { replyService.delete(reply) }
    }

    @Test
    fun `should return 403 when deleting reply if user is not the author`() {
        every { replyService.isUserAuthor(reply, user) } returns false

        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!, reply.id!!))
            .andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value(ErrorCode.USER_ACTION_DENIED.name) }
                jsonPath("$.details") { value("user is not the author of the reply") }
            }
    }

    @Test
    fun `should add like to reply`() {
        every { replyService.hasUserLikedReply(reply, user) } returns false
        every { replyService.addLike(reply, user) } returns mockk()

        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!, reply.id!!, "likes"))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("User's like added successfully to comment ${reply.id}"))
    }

    @Test
    fun `should return 400 when user has already liked the reply`() {
        every { replyService.hasUserLikedReply(reply, user) } returns true

        mockMvc.perform(
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!, reply.id!!, "likes"))
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

        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!, reply.id!!, "likes"))
            .andExpect {
                status { isOk() }
                jsonPath("$.message") { value("User's like removed successfully to reply ${reply.id}") }
            }
    }

    @Test
    fun `should return 400 when removing like from reply user hasn't liked`() {
        every { replyService.hasUserLikedReply(reply, user) } returns false

        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!, reply.id!!, "likes"))
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
            multipart(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!, invalidReplyId, "likes"))
                .contentType(MediaType.APPLICATION_JSON)
                .with { it.method = "PUT"; it }
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when removing like from non-existing reply`() {
        val invalidReplyId = "invalid-reply-id"
        every { replyService.find(invalidReplyId) } returns null

        mockMvc.delete(buildReplyApiUrl(topic.id!!, post.id!!, comment.id!!, invalidReplyId, "likes"))
            .andExpect {
                status { isNotFound() }
            }
    }
}

