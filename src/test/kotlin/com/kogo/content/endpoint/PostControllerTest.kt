package com.kogo.content.endpoint

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationResponse
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.service.UserContextService
import com.kogo.content.service.pagination.PageToken
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.endpoint.`test-util`.Fixture
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable Spring Security filter chain during testing
class PostControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean
    lateinit var postService: PostService

    @MockkBean
    lateinit var topicService: TopicService

    @MockkBean
    lateinit var userService: UserContextService

    /**
     * Fixtures
     */
    private final val user: UserDetails = Fixture.createUserFixture()
    private final val topic: Topic = Fixture.createTopicFixture(user)
    private final val post: Post = Fixture.createPostFixture(topic = topic, author = user)

    private fun buildPostApiUrl(topicId: String, vararg paths: String, params: Map<String, String> = emptyMap() ): String {
        val baseUrl = "/media/topics/$topicId/posts"
        val url = if (paths.isNotEmpty()) "${baseUrl}/" + paths.joinToString("/") else baseUrl
        val paramBuilder = StringBuilder()
        params.forEach { paramBuilder.append(it.key, "=", it.value, "&") }
        return "$url?$paramBuilder"
    }

    @BeforeEach
    fun setup() {
        every { userService.getCurrentUserDetails() } returns user
        every { topicService.find(topic.id!!) } returns topic
        every { postService.find(post.id!!) } returns post

        // temporary for building user activity response
        every { postService.findLike(any(), any()) } returns null
        every { postService.findView(any(), any()) } returns null
    }

    @Test
    fun `should return posts with pagination metadata by topic id`() {
        val topicId = topic.id!!
        val posts = listOf(
            Fixture.createPostFixture(topic = topic, author = user),
            Fixture.createPostFixture(topic = topic, author = user))

        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken())
        val nextPageToken = paginationRequest.pageToken.nextPageToken("sample-next-page-token")
        val paginationResponse = PaginationResponse(posts, nextPageToken)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { postService.listPostsByTopicId(topicId, capture(paginationRequestSlot)) } returns paginationResponse

        mockMvc.get(buildPostApiUrl(topicId = topicId, params = mapOf("limit" to "${paginationRequest.limit}")))
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.length()") { value(posts.size) } }
            .andExpect { header { string(PaginationResponse.HEADER_NAME_PAGE_TOKEN, nextPageToken.toString()) } }
            .andExpect { header { string(PaginationResponse.HEADER_NAME_PAGE_SIZE, "${paginationRequest.limit}") } }

        val capturedPaginationRequest = paginationRequestSlot.captured
        assertThat(capturedPaginationRequest.limit).isEqualTo(paginationRequest.limit)
        assertThat(capturedPaginationRequest.pageToken.toString()).isEqualTo(paginationRequest.pageToken.toString())
    }

    @Test
    fun `should return a single post`() {
        val topicId = topic.id!!
        val postId = post.id!!

        every { postService.addView(post, user) } returns mockk()

        mockMvc.get(buildPostApiUrl(topicId, postId))
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.id") { value(postId) } }
            .andExpect { jsonPath("$.data.title") { value(post.title) } }
            .andExpect { jsonPath("$.data.createdAt").exists() }
    }

    @Test
    fun `should increment view count when return a single post`() {
        val topicId = topic.id!!
        val postId = post.id!!

        val postSlot = slot<Post>()
        every { postService.addView(capture(postSlot), user) } returns mockk()

        mockMvc.get(buildPostApiUrl(topicId, postId)).andExpect { status { isOk() } }
        val postCaptured = postSlot.captured
        assertThat(postCaptured.id).isEqualTo(postId)
    }

    @Test
    fun `should return 404 when post id is not found`() {
        val postId = "invalid-post-id"

        every { postService.find(postId) } returns null

        mockMvc.get(buildPostApiUrl(topic.id!!, postId))
            .andExpect { status { isNotFound() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
    }

    @Test
    fun `should create a new post if user is a topic member`() {
        val topicId = topic.id!!

        every { topicService.isUserFollowingTopic(topic, user) } returns true // User is a topic member
        every { postService.create(topic, user, any()) } returns post

        mockMvc.perform(
            multipart(buildPostApiUrl(topicId))
                .part(MockPart("title", post.title.toByteArray()))
                .part(MockPart("content", post.content.toByteArray()))
                .file(MockMultipartFile("images", "image.png", "image/png", "some image".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it })
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.title").value(post.title))
            .andExpect(jsonPath("$.data.content").value(post.content))
            .andExpect(jsonPath("$.data.topicId").value(topic.id))
            .andExpect { jsonPath("$.data.createdAt").exists() }
    }

    @Test
    fun `should return 403 when create a new post in a topic which user is not following`() {
        val topicId = topic.id!!

        every { topicService.isUserFollowingTopic(topic, user) } returns false // User is not a topic member

        mockMvc.perform(
            multipart(buildPostApiUrl(topicId))
                .part(MockPart("title", "new post".toByteArray()))
                .part(MockPart("content", "post content".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value(ErrorCode.USER_ACTION_DENIED.name))
            .andExpect(jsonPath("$.details").value("user is not following topic id: $topicId"))
    }

    @Test
    fun `should return 404 when topic is not found`() {
        val topicId = "invalid-topic-id"

        every { topicService.find(topicId) } returns null

        mockMvc.perform(
            multipart(buildPostApiUrl(topicId))
                .part(MockPart("title", "some post title".toByteArray()))
                .part(MockPart("content", "some post content".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it })
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should update an existing post if user is the post owner`() {
        val topicId = topic.id!!
        val postId = post.id!!
        val updatedPost = post.copy()
        updatedPost.title = "updated post title"
        updatedPost.content = "updated post content"

        every { userService.getCurrentUserDetails() } returns user
        every { postService.isPostAuthor(post, user) } returns true // User is the post owner
        every { postService.update(post, any()) } returns updatedPost

        mockMvc.perform(
            multipart(buildPostApiUrl(topicId, postId))
                .part(MockPart("title", updatedPost.title.toByteArray()))
                .part(MockPart("content", updatedPost.content.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it })
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.title").value(updatedPost.title))
            .andExpect(jsonPath("$.data.content").value(updatedPost.content))
            .andExpect(jsonPath("$.data.topicId").value(topicId))
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
    }

    @Test
    fun `should return 403 when updating a post if user is not the post owner`() {
        val topicId = topic.id!!
        val postId = post.id!!
        val differentUser = Fixture.createUserFixture() // Different user from the post owner

        every { userService.getCurrentUserDetails() } returns differentUser // Different user
        every { postService.isPostAuthor(post, differentUser) } returns false // User is not the post owner

        mockMvc.perform(
            multipart(buildPostApiUrl(topicId, postId))
                .part(MockPart("title", "updated post title".toByteArray()))
                .part(MockPart("content", "updated post content".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it })
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value(ErrorCode.USER_ACTION_DENIED.name))
            .andExpect(jsonPath("$.details").value("user is not the author of this post"))
    }

    @Test
    fun `should return 404 when updating non-existing post`() {
        val topicId = topic.id!!
        val postId = "invalid-post-id"

        every { postService.find(postId) } returns null

        mockMvc.perform(
            multipart(buildPostApiUrl(topicId, postId))
                .part(MockPart("title", "some post title".toByteArray()))
                .part(MockPart("content", "some post content".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it })
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should delete a post if user is the post owner`() {
        val topicId = topic.id!!
        val postId = post.id!!
        val differentUser = Fixture.createUserFixture() // Different user from the topic owner
        post.author = differentUser

        every { userService.getCurrentUserDetails() } returns differentUser
        every { postService.isPostAuthor(post, differentUser) } returns true // User is the post owner
        every { topicService.isTopicOwner(topic, differentUser) } returns false // User is not topic owner
        every { postService.delete(post) } returns Unit

        mockMvc.delete(buildPostApiUrl(topicId, postId))
            .andExpect { status { isOk() } }
    }

    @Test
    fun `should delete the post if user is the topic owner`() {
        val postId = post.id!!
        val topicId = topic.id!!
        val differentUser = Fixture.createUserFixture()
        post.author = differentUser

        // Mocking repository and service responses
        every { postService.isPostAuthor(post, user) } returns false // User is not the post owner
        every { topicService.isTopicOwner(topic, user) } returns true // User is the topic owner
        every { postService.delete(post) } returns Unit

        mockMvc.delete(buildPostApiUrl(topicId, postId))
            .andExpect{ status().isOk }
    }

    @Test
    fun `should return 403 if user is neither post owner nor topic owner`() {
        val postId = post.id!!
        val topicId = topic.id!!
        val anonymousUser = Fixture.createUserFixture() // A different user who is neither the post nor topic owner

        every { userService.getCurrentUserDetails() } returns anonymousUser
        every { postService.isPostAuthor(post, anonymousUser) } returns false // User is not the post owner
        every { topicService.isTopicOwner(topic, anonymousUser) } returns false // User is not the topic owner

        mockMvc.delete(buildPostApiUrl(topicId, postId))
            .andExpect{ status().isForbidden }
            .andExpect{jsonPath("$.error.reason").value(ErrorCode.USER_ACTION_DENIED)}
    }

    @Test
    fun `should return 404 when deleting non-existing post`() {
        val topicId = topic.id!!
        val postId = "invalid-post-id"

        every { postService.find(postId) } returns null

        mockMvc.delete(buildPostApiUrl(topicId, postId))
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `should create a like under the post`() {
        val postId = post.id!!

        every { postService.hasUserLikedPost(post, user) } returns false
        every { postService.addLike(post, user) } returns mockk()

        mockMvc.perform(
            multipart(buildPostApiUrl(topic.id!!, postId, "likes"))
                .contentType(MediaType.APPLICATION_JSON)
                .with{ it.method = "PUT"; it })
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.error.reason").doesNotExist())
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.id").value(postId))
    }

    @Test
    fun `should return 404 when creating a like to a non-existing post`() {
        val postId = "invalid-post-id"

        every { postService.find(postId) } returns null

        mockMvc.perform(
            multipart(buildPostApiUrl(topic.id!!, postId, "likes"))
                .contentType(MediaType.APPLICATION_JSON)
                .with{ it.method = "PUT"; it })
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 if user has already liked a post`() {
        val postId = post.id!!

        every { postService.hasUserLikedPost(post, user) } returns true

        mockMvc.perform(
            multipart(buildPostApiUrl(topic.id!!, postId, "likes"))
                .contentType(MediaType.APPLICATION_JSON)
                .with{ it.method = "PUT"; it })
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value(ErrorCode.BAD_REQUEST.name))
            .andExpect(jsonPath("$.details").value("user already liked this post: $postId"))
    }

    @Test
    fun `should delete a like from a post`() {
        val postId = post.id!!

        every { postService.hasUserLikedPost(post, user) } returns true
        every { postService.removeLike(post, user) } returns Unit

        mockMvc.delete(buildPostApiUrl(topic.id!!, postId, "likes"))
            .andExpect { status { isOk() } }
            .andExpect{ jsonPath("$.message").value("User's like deleted successfully to post: $postId")}
    }

    @Test
    fun `should return 404 when deleting a like under non-existing post`() {
        val postId = "invalid-post-id"

        every { postService.find(postId) } returns null

        mockMvc.delete(buildPostApiUrl(topic.id!!, postId, "likes"))
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `should return 400 when deleting like from a post which user has not liked`() {
        val postId = post.id!!

        every { postService.hasUserLikedPost(post, user) } returns false
        every { postService.removeLike(post, user) } returns Unit

        mockMvc.delete(buildPostApiUrl(topic.id!!, postId, "likes"))
            .andExpect{ status { isBadRequest()} }
            .andExpect{ jsonPath("$.details").value("user never liked this post: $postId") }
    }
}
