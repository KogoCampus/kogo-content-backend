package com.kogo.content.endpoint

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.model.PaginationRequest
import com.kogo.content.endpoint.model.PaginationResponse
import com.kogo.content.searchengine.SearchIndexService
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.service.UserContextService
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.util.fixture
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
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
import java.time.Instant

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

    @MockkBean
    lateinit var searchIndexService: SearchIndexService

    @Test
    fun `should return a paginated list of posts by topic id`() {
        val topic = createTopicFixture()
        val posts = listOf(createPostFixture(topic), createPostFixture(topic))
        val topicId = topic.id!!
        val paginationRequest = PaginationRequest(limit = 2, page = null)
        val paginationResponse = PaginationResponse(posts, "sample-next-page-token")
        val paginationRequestSlot = slot<PaginationRequest>()
        every { topicService.find(topicId) } returns topic
        every { postService.listPostsByTopicId(topicId, capture(paginationRequestSlot)) } returns paginationResponse
        mockMvc.get(buildPostApiUrl(topicId = topicId, params = mapOf("limit" to "${paginationRequest.limit}")))
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.length()") { value(posts.size) } }
            .andExpect { header { string("next_page", "sample-next-page-token") } }
            .andExpect { header { string("item_count", "${paginationRequest.limit}") } }
        val capturedPaginationRequest = paginationRequestSlot.captured
        assertThat(capturedPaginationRequest.limit).isEqualTo(paginationRequest.limit)
        assertThat(capturedPaginationRequest.page).isEqualTo(paginationRequest.page)
    }

    @Test
    fun `should return a single post by post id`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val topicId = topic.id!!
        val postId = post.id!!
        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns post
        mockMvc.get(buildPostApiUrl(topicId, postId))
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.id") { value(postId) } }
            .andExpect { jsonPath("$.data.title") { value(post.title) } }
            .andExpect { jsonPath("$.data.createdAt").exists() }
    }

    @Test
    fun `should return 404 when post id is not found`() {
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val postId = "invalid-post-id"
        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns null
        mockMvc.get(buildPostApiUrl(topicId, postId))
            .andExpect { status { isNotFound() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
    }

    @Test
    fun `should create a new post if user is a topic member`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val user = createUserFixture()
        val topicId = topic.id!!

        // Mocking repository and service responses
        every { topicService.find(topicId) } returns topic
        every { userService.getCurrentUserDetails() } returns user
        every { topicService.existsFollowingByUserIdAndTopicId(user.id!!, topicId) } returns true // User is a topic member
        every { postService.create(topic, user, any()) } returns post
        every { searchIndexService.addDocument(any(), any()) } returns Unit

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
    fun `should return 403 if user is not a topic member`() {
        val topic = createTopicFixture()
        val user = createUserFixture()
        val topicId = topic.id!!

        // Mocking repository and service responses
        every { topicService.find(topicId) } returns topic
        every { userService.getCurrentUserDetails() } returns user
        every { topicService.existsFollowingByUserIdAndTopicId(user.id!!, topicId) } returns false // User is not a topic member

        mockMvc.perform(
            multipart(buildPostApiUrl(topicId))
                .part(MockPart("title", "new post".toByteArray()))
                .part(MockPart("content", "post content".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.reason").value("USER_IS_NOT_MEMBER"))
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
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val post = createPostFixture(topic)
        val postId = post.id!!
        val user = post.owner
        val updatedPost = Post(
            id = postId,
            title = "updated post title",
            content = "updated post content",
            topic = post.topic,
            owner = post.owner,
            comments = post.comments,
            createdAt = Instant.now()
        )

        // Mocking repository and service responses
        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { postService.isPostOwner(post, user) } returns true // User is the post owner
        every { postService.update(post, any()) } returns updatedPost
        every { searchIndexService.updateDocument(any(), any()) } returns Unit

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
    }

    @Test
    fun `should return 403 if user is not the post owner`() {
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val post = createPostFixture(topic)
        val postId = post.id!!
        val differentUser = createUserFixture() // Different user from the post owner

        // Mocking repository and service responses
        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns differentUser // Different user
        every { postService.isPostOwner(post, differentUser) } returns false // User is not the post owner

        mockMvc.perform(
            multipart(buildPostApiUrl(topicId, postId))
                .part(MockPart("title", "updated post title".toByteArray()))
                .part(MockPart("content", "updated post content".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it })
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.reason").value("USER_IS_NOT_OWNER"))
    }

    @Test
    fun `should return 404 when updating non-existing post`() {
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val postId = "invalid-post-id"
        every { topicService.find(topicId) } returns topic
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
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val post = createPostFixture(topic)
        val postId = post.id!!
        val user = post.owner

        // Mocking repository and service responses
        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { postService.isPostOwner(post, user) } returns true // User is the post owner
        every { topicService.isTopicOwner(topic, user) } returns false // User is not topic owner
        every { postService.delete(post) } returns Unit
        every { searchIndexService.deleteDocument(any(), any()) } returns Unit

        mockMvc.delete(buildPostApiUrl(topicId, postId))
            .andExpect { status { isOk() } }
    }

    @Test
    fun `should delete the post if user is the topic owner`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val postId = post.id!!
        val topicId = topic.id!!
        val user = createUserFixture() // A different user who is the topic owner

        // Mocking repository and service responses
        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { postService.isPostOwner(post, user) } returns false // User is not the post owner
        every { topicService.isTopicOwner(topic, user) } returns true // User is the topic owner
        every { postService.delete(post) } returns Unit
        every { searchIndexService.deleteDocument(any(), any()) } returns Unit

        mockMvc.delete(buildPostApiUrl(topicId, postId))
            .andExpect{ status().isOk }
    }

    @Test
    fun `should return 403 if user is neither post owner nor topic owner`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val postId = post.id!!
        val topicId = topic.id!!
        val user = createUserFixture() // A different user who is neither the post nor topic owner

        // Mocking repository and service responses
        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { postService.isPostOwner(post, user) } returns false // User is not the post owner
        every { topicService.isTopicOwner(topic, user) } returns false // User is not the topic owner

        mockMvc.delete(buildPostApiUrl(topicId, postId))
            .andExpect{ status().isForbidden }
            .andExpect{jsonPath("$.error.reason").value("USER_IS_NOT_OWNER")}
    }

    @Test
    fun `should return 404 when deleting non-existing post`() {
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val postId = "invalid-post-id"
        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns null
        mockMvc.delete(buildPostApiUrl(topicId, postId))
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `should create a like under the post`() {
        val post = createPostFixture(createTopicFixture())
        val postId = post.id!!
        val user = createUserFixture()
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { postService.findLikeByUserIdAndParentId(user.id!!, postId) } returns null
        every { postService.addLike(postId, user) } returns Unit
        mockMvc.perform(
            multipart("/media/posts/$postId/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .with{ it.method = "POST"; it })
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.error.reason").doesNotExist())
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.id").value(postId))
    }

    @Test
    fun `should return 404 when creating a like under non-existing post`() {
        val postId = "invalid-post-id"
        val user = createUserFixture()
        every { postService.find(postId) } returns null
        every { userService.getCurrentUserDetails() } returns user
        mockMvc.perform(
            multipart("/media/posts/$postId/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .with{ it.method = "POST"; it })
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 when creating a duplicate like under the same post`() {
        val post = createPostFixture(createTopicFixture())
        val postId = post.id!!
        val user = createUserFixture()
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { postService.findLikeByUserIdAndParentId(user.id!!, postId) } returns mockk()
        mockMvc.perform(
            multipart("/media/posts/$postId/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .with{ it.method = "POST"; it })
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value(ErrorCode.BAD_REQUEST.name))
            .andExpect(jsonPath("$.details").value("user already liked this post: $postId"))
    }

    @Test
    fun `should delete a like under the post`() {
        val post = createPostFixture(createTopicFixture())
        val postId = post.id!!
        val user = createUserFixture()
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { postService.findLikeByUserIdAndParentId(user.id!!, postId) } returns mockk()
        every { postService.removeLike(postId, user) } returns Unit
        mockMvc.delete("/media/posts/$postId/likes")
            .andExpect { status { isOk() } }
            .andExpect{ jsonPath("$.message").value("User's like deleted successfully to post: $postId")}
    }

    @Test
    fun `should return 404 when deleting a like under non-existing post`() {
        val postId = "invalid-post-id"
        val user = createUserFixture()
        every { postService.find(postId) } returns null
        every { userService.getCurrentUserDetails() } returns user
        mockMvc.delete("/media/posts/$postId/likes")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `should return 400 when deleting non-existing like`() {
        val post = createPostFixture(createTopicFixture())
        val postId = post.id!!
        val user = createUserFixture()
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { postService.findLikeByUserIdAndParentId(user.id!!, postId) } returns null
        mockMvc.delete("/media/posts/$postId/likes")
            .andExpect{ status { isBadRequest()} }
            .andExpect{ jsonPath("$.details").value("user never liked this post: $postId") }
    }

    @Test
    fun `should create a view under the post`() {
        val post = createPostFixture(createTopicFixture())
        val postId = post.id!!
        val user = createUserFixture()
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { postService.findViewByUserIdAndParentId(user.id!!, postId) } returns null
        every { postService.addView(postId, user) } returns Unit
        mockMvc.perform(
            multipart("/media/posts/$postId/views")
                .contentType(MediaType.APPLICATION_JSON)
                .with{ it.method = "POST"; it })
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.error.reason").doesNotExist())
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.id").value(postId))
    }

    @Test
    fun `should return 404 when creating a view under non-existing post`() {
        val postId = "invalid-post-id"
        val user = createUserFixture()
        every { postService.find(postId) } returns null
        every { userService.getCurrentUserDetails() } returns user
        mockMvc.perform(
            multipart("/media/posts/$postId/views")
                .contentType(MediaType.APPLICATION_JSON)
                .with{ it.method = "POST"; it })
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 when creating a duplicate view under the same post`() {
        val post = createPostFixture(createTopicFixture())
        val postId = post.id!!
        val user = createUserFixture()
        every { postService.find(postId) } returns post
        every { userService.getCurrentUserDetails() } returns user
        every { postService.findViewByUserIdAndParentId(user.id!!, postId) } returns mockk()
        mockMvc.perform(
            multipart("/media/posts/$postId/views")
                .contentType(MediaType.APPLICATION_JSON)
                .with{ it.method = "POST"; it })
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value(ErrorCode.BAD_REQUEST.name))
            .andExpect(jsonPath("$.details").value("user already viewed this post: $postId"))
    }

    private fun buildPostApiUrl(topicId: String, vararg paths: String, params: Map<String, String> = emptyMap() ): String {
        val baseUrl = "/media/topics/$topicId/posts"
        val url = if (paths.isNotEmpty()) "${baseUrl}/" + paths.joinToString("/") else baseUrl
        val paramBuilder = StringBuilder()
        params.forEach { paramBuilder.append(it.key, "=", it.value, "&") }
        return "$url?$paramBuilder"
    }

    private fun createTopicFixture() = fixture<Topic> { mapOf(
        "owner" to createUserFixture()
    ) }

    private fun createPostFixture(topic: Topic) = fixture<Post> {
        mapOf(
            "topic" to topic,
            "owner" to createUserFixture(),
            "comments" to emptyList<Any>(),
            "attachments" to emptyList<Any>(),
            "createdAt" to Instant.now()
        )
    }

    private fun createUserFixture() = fixture<UserDetails>()
}
