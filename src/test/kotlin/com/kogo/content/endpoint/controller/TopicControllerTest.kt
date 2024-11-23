package com.kogo.content.endpoint.controller

import com.kogo.content.common.CursorValue
import com.kogo.content.common.PageToken
import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.service.UserService
import com.kogo.content.service.TopicService
import com.kogo.content.storage.entity.User
import com.kogo.content.storage.entity.Topic
import com.kogo.content.endpoint.`test-util`.Fixture
import com.kogo.content.service.PostService
import com.kogo.content.storage.view.TopicAggregate
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable Spring Security filter chain during testing
class TopicControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    companion object {
        private const val TOPIC_API_BASE_URL = "/media/topics"
    }

    @MockkBean
    lateinit var topicService: TopicService

    @MockkBean
    lateinit var postService: PostService

    @MockkBean
    lateinit var userService: UserService

    lateinit var user: User
    lateinit var topic: Topic
    lateinit var topicAggregate: TopicAggregate

    private fun buildTopicApiUrl(vararg paths: String, params: Map<String, String> = emptyMap()): String {
        val baseUrl = TOPIC_API_BASE_URL
        val url = if (paths.isNotEmpty()) "$baseUrl/${paths.joinToString("/")}" else baseUrl
        val paramBuilder = StringBuilder()
        params.forEach { paramBuilder.append("${it.key}=${it.value}&") }
        return if (paramBuilder.isNotEmpty()) "$url?$paramBuilder" else url
    }

    @BeforeEach
    fun setup() {
        user = Fixture.createUserFixture()
        topic = Fixture.createTopicFixture(user)
        topicAggregate = Fixture.createTopicAggregateFixture(topic)

        every { userService.getCurrentUser() } returns user
        every { topicService.findAggregate(topic.id!!) } returns topicAggregate
        every { topicService.hasUserFollowedTopic(topic, user) } returns false
    }

    @Test
    fun `should return a topic by id`() {
        val topicId = topic.id!!

        every { topicService.find(topicId) } returns topic

        mockMvc.get(buildTopicApiUrl(topicId))
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.data.id") { value(topicId) }
                jsonPath("$.data.topicName") { value(topic.topicName) }
                jsonPath("$.data.followerCount") { value(topicAggregate.followerCount) }
                jsonPath("$.data.postCount") { value(topicAggregate.postCount) }
                jsonPath("$.data.followedByCurrentUser") { value(false) }
                jsonPath("$.data.createdAt").exists()
            }
    }

    @Test
    fun `should create a new topic`() {
        every { topicService.findTopicByTopicName(topic.topicName) } returns null
        every { topicService.create(any(), user) } returns topic
        every { topicService.follow(topic, user) } returns topic

        mockMvc.perform(
            multipart(buildTopicApiUrl())
                .part(MockPart("topicName", topic.topicName.toByteArray()))
                .part(MockPart("description", topic.description.toByteArray()))
                .part(MockPart("tags", topic.tags.first().toByteArray()))
                .file(MockMultipartFile("profileImage", "image.jpeg", "image/jpeg", "some image".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.topicName").value(topic.topicName))
            .andExpect(jsonPath("$.data.description").value(topic.description))
            .andExpect(jsonPath("$.data.followerCount").value(topicAggregate.followerCount))
            .andExpect(jsonPath("$.data.postCount").value(topicAggregate.postCount))
            .andExpect(jsonPath("$.data.followedByCurrentUser").value(false))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }

    @Test
    fun `should follow a topic`() {
        val topicId = topic.id!!

        every { topicService.find(topicId) } returns topic
        every { topicService.hasUserFollowedTopic(topic, user) } returns false
        every { topicService.follow(topic, user) } returns topic

        mockMvc.perform(
            put(buildTopicApiUrl(topicId, "follow"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.id").value(topicId))
            .andExpect(jsonPath("$.data.followerCount").value(topicAggregate.followerCount))
            .andExpect(jsonPath("$.data.followedByCurrentUser").value(false))
            .andExpect(jsonPath("$.message").value("User's follow added successfully to topic: $topicId"))
    }

    @Test
    fun `should unfollow a topic`() {
        val topicId = topic.id!!

        every { topicService.find(topicId) } returns topic
        every { topicService.isUserTopicOwner(topic, user) } returns false
        every { topicService.hasUserFollowedTopic(topic, user) } returns true
        every { topicService.unfollow(topic, user) } returns topic

        mockMvc.perform(
            put(buildTopicApiUrl(topicId, "unfollow"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.id").value(topicId))
            .andExpect(jsonPath("$.message").value("User's follow successfully removed from topic: $topicId"))
    }

    @Test
    fun `should return 404 when topic is not found`() {
        val topicId = "invalid-id"
        every { topicService.find(topicId) } returns null
        mockMvc.get(buildTopicApiUrl(topicId))
            .andExpect { status { isNotFound() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
    }

    @Test
    fun `should return 400 when topic name is not unique`() {
        val topicName = "existing-topic"
        every { topicService.findTopicByTopicName(topicName) } returns topic
        mockMvc.perform(
            multipart(buildTopicApiUrl())
                .part(MockPart("topicName", topicName.toByteArray()))
                .part(MockPart("description", "some description".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it })
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should update an existing topic`() {
        val topicId = topic.id!!
        val updatedAt = Instant.now()
        val updatedTopic = Topic(
            id = topicId,
            topicName = "updated topic name",
            description = "updated description",
            owner = topic.owner,
            createdAt = topic.createdAt,
            updatedAt = updatedAt
        )
        topicAggregate.topic.topicName = updatedTopic.topicName
        topicAggregate.topic.description = updatedTopic.description

        every { topicService.find(topicId) } returns topic
        every { userService.getCurrentUser() } returns user
        every { topicService.isUserTopicOwner(topic, user) } returns true
        every { topicService.findTopicByTopicName("updated topic name") } returns null
        every { topicService.update(topic, any()) } returns updatedTopic
        every { topicService.hasUserFollowedTopic(updatedTopic, user) } returns true

        mockMvc.perform(
            multipart(buildTopicApiUrl(topicId))
                .part(MockPart("topicName", updatedTopic.topicName.toByteArray()))
                .part(MockPart("description", updatedTopic.description.toByteArray()))
                .file(MockMultipartFile("profileImage", "image.jpeg", "image/jpeg", "some image".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it })
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.topicName").value(updatedTopic.topicName))
            .andExpect(jsonPath("$.data.description").value(updatedTopic.description))
            .andExpect { jsonPath("$.data.createdAt").exists() }
            .andExpect { jsonPath("$.data.createdAt").exists() }
            .andExpect(jsonPath("$.error.reason").doesNotExist())
    }

    @Test
    fun `should return 400 when updating topic name is not unique`() {
        val topicId = topic.id!!
        val topicUpdate = TopicUpdate(
            topicName = "duplicate topic name",
            description = "updated description"
        )

        // Mocking the necessary service responses
        every { topicService.find(topicId) } returns topic
        every { userService.getCurrentUser() } returns user
        every { topicService.isUserTopicOwner(topic, user) } returns true
        every { topicService.findTopicByTopicName("duplicate topic name") } returns topic // Duplicate topic name found

        mockMvc.perform(
            multipart(buildTopicApiUrl(topicId))
                .part(MockPart("topicName", topicUpdate.topicName!!.toByteArray()))
                .part(MockPart("description", topicUpdate.description!!.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it })
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.details").value("topic name must be unique: ${topicUpdate.topicName}"))
    }

    @Test
    fun `should return 404 when updating non-existing topic`() {
        val topicId = "invalid-id"
        every { topicService.find(topicId) } returns null
        mockMvc.perform(
            multipart(buildTopicApiUrl(topicId))
                .part(MockPart("topicName", "some topic name".toByteArray()))
                .part(MockPart("description", "some description".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it })
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should delete a topic`() {
        val topicId = topic.id!!

        every { topicService.find(topicId) } returns topic
        every { userService.getCurrentUser() } returns user
        every { topicService.isUserTopicOwner(topic, user) } returns true
        every { topicService.delete(topic) } returns Unit

        mockMvc.delete(buildTopicApiUrl(topicId))
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.error.reason").doesNotExist() } // Ensure no error is present
            .andExpect { jsonPath("$.data").exists() }
    }

    @Test
    fun `should return 404 when deleting non-existing topic`() {
        val topicId = "invalid-id"
        every { topicService.find(topicId) } returns null
        mockMvc.delete(buildTopicApiUrl(topicId))
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `should return 403 when creating post if user is not following the topic`() {
        val topicId = topic.id!!

        every { topicService.find(topicId) } returns topic
        every { topicService.hasUserFollowedTopic(topic, user) } returns false

        mockMvc.perform(
            multipart(buildTopicApiUrl(topicId, "posts"))
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
    fun `should delete post if user is topic owner`() {
        val topicId = topic.id!!
        val postId = "post-id"
        val post = Fixture.createPostFixture(topic = topic, author = user)

        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns post
        every { postService.isPostAuthor(post, user) } returns false
        every { topicService.isUserTopicOwner(topic, user) } returns true
        every { postService.delete(post) } returns Unit

        mockMvc.delete(buildTopicApiUrl(topicId, "posts", postId))
            .andExpect { status { isOk() } }
    }

    @Test
    fun `should delete post if user is post author`() {
        val topicId = topic.id!!
        val postId = "post-id"
        val post = Fixture.createPostFixture(topic = topic, author = user)

        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns post
        every { postService.isPostAuthor(post, user) } returns true
        every { topicService.isUserTopicOwner(topic, user) } returns false
        every { postService.delete(post) } returns Unit

        mockMvc.delete(buildTopicApiUrl(topicId, "posts", postId))
            .andExpect { status { isOk() } }
    }

    @Test
    fun `should return 403 when deleting post if user is neither post author nor topic owner`() {
        val topicId = topic.id!!
        val postId = "post-id"
        val post = Fixture.createPostFixture(topic = topic, author = user)

        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns post
        every { postService.isPostAuthor(post, user) } returns false
        every { topicService.isUserTopicOwner(topic, user) } returns false

        mockMvc.delete(buildTopicApiUrl(topicId, "posts", postId))
            .andExpect { status { isForbidden() } }
            .andExpect { jsonPath("$.error").value(ErrorCode.USER_ACTION_DENIED.name) }
            .andExpect { jsonPath("$.details").value("user is not the author of this post or the topic owner") }
    }

    @Test
    fun `should return 404 when deleting non-existing post in topic`() {
        val topicId = topic.id!!
        val invalidPostId = "invalid-post-id"

        every { topicService.find(topicId) } returns topic
        every { postService.find(invalidPostId) } returns null

        mockMvc.delete(buildTopicApiUrl(topicId, "posts", invalidPostId))
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `should return a list of topics with pagination`() {
        val pageToken = PageToken.create()
        val limit = 10
        val paginationParams = mapOf(
            PaginationRequest.PAGE_TOKEN_PARAM to pageToken.encode(),
            PaginationRequest.PAGE_SIZE_PARAM to limit.toString()
        )

        val topics = listOf(
            Fixture.createTopicAggregateFixture(topic),
            Fixture.createTopicAggregateFixture(Fixture.createTopicFixture(user))
        )

        val nextPageToken = PageToken(
            cursors = mapOf("createdAt" to CursorValue.from(Instant.now()))
        )

        val paginationSlice = PaginationSlice(
            items = topics,
            nextPageToken = nextPageToken
        )

        every { topicService.getAllTopics(any()) } returns paginationSlice

        mockMvc.get(buildTopicApiUrl(params = paginationParams))
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                header { string(PaginationSlice.HEADER_PAGE_TOKEN, nextPageToken.encode()) }
                header { string(PaginationSlice.HEADER_PAGE_SIZE, topics.size.toString()) }
                jsonPath("$.data.length()") { value(topics.size) }
                jsonPath("$.data[0].id") { value(topics[0].topicId) }
                jsonPath("$.data[0].topicName") { value(topics[0].topic.topicName) }
            }
    }
}
