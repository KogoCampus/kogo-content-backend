package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.CursorValue
import com.kogo.content.endpoint.common.PageToken
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.model.GroupUpdate
import com.kogo.content.service.UserService
import com.kogo.content.service.GroupService
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.model.entity.Group
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
class GroupControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    companion object {
        private const val TOPIC_API_BASE_URL = "/media/topics"
    }

    @MockkBean
    lateinit var groupService: GroupService

    @MockkBean
    lateinit var postService: PostService

    @MockkBean
    lateinit var userService: UserService

    lateinit var user: User
    lateinit var group: Group
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
        group = Fixture.createTopicFixture(user)
        topicAggregate = Fixture.createTopicAggregateFixture(group)

        every { userService.findCurrentUser() } returns user
        every { groupService.findAggregate(group.id!!) } returns topicAggregate
        every { groupService.hasUserFollowedTopic(group, user) } returns false
    }

    @Test
    fun `should return a topic by id`() {
        val topicId = group.id!!

        every { groupService.find(topicId) } returns group

        mockMvc.get(buildTopicApiUrl(topicId))
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.data.id") { value(topicId) }
                jsonPath("$.data.topicName") { value(group.groupName) }
                jsonPath("$.data.followerCount") { value(topicAggregate.followerCount) }
                jsonPath("$.data.postCount") { value(topicAggregate.postCount) }
                jsonPath("$.data.followedByCurrentUser") { value(false) }
                jsonPath("$.data.createdAt").exists()
            }
    }

    @Test
    fun `should create a new topic`() {
        every { groupService.findByGroupName(group.groupName) } returns null
        every { groupService.create(any(), user) } returns group
        every { groupService.follow(group, user) } returns group

        mockMvc.perform(
            multipart(buildTopicApiUrl())
                .part(MockPart("topicName", group.groupName.toByteArray()))
                .part(MockPart("description", group.description.toByteArray()))
                .part(MockPart("tags", group.tags.first().toByteArray()))
                .file(MockMultipartFile("profileImage", "image.jpeg", "image/jpeg", "some image".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it }
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.topicName").value(group.groupName))
            .andExpect(jsonPath("$.data.description").value(group.description))
            .andExpect(jsonPath("$.data.followerCount").value(topicAggregate.followerCount))
            .andExpect(jsonPath("$.data.postCount").value(topicAggregate.postCount))
            .andExpect(jsonPath("$.data.followedByCurrentUser").value(false))
            .andExpect(jsonPath("$.data.createdAt").exists())
    }

    @Test
    fun `should follow a topic`() {
        val topicId = group.id!!

        every { groupService.find(topicId) } returns group
        every { groupService.hasUserFollowedTopic(group, user) } returns false
        every { groupService.follow(group, user) } returns group

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
        val topicId = group.id!!

        every { groupService.find(topicId) } returns group
        every { groupService.isUserTopicOwner(group, user) } returns false
        every { groupService.hasUserFollowedTopic(group, user) } returns true
        every { groupService.unfollow(group, user) } returns group

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
        every { groupService.find(topicId) } returns null
        mockMvc.get(buildTopicApiUrl(topicId))
            .andExpect { status { isNotFound() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
    }

    @Test
    fun `should return 400 when topic name is not unique`() {
        val topicName = "existing-topic"
        every { groupService.findByGroupName(topicName) } returns group
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
        val topicId = group.id!!
        val updatedAt = Instant.now()
        val updatedGroup = Group(
            id = topicId,
            groupName = "updated topic name",
            description = "updated description",
            owner = group.owner,
            createdAt = group.createdAt,
            updatedAt = updatedAt
        )
        topicAggregate.group.groupName = updatedGroup.groupName
        topicAggregate.group.description = updatedGroup.description

        every { groupService.find(topicId) } returns group
        every { userService.findCurrentUser() } returns user
        every { groupService.isUserTopicOwner(group, user) } returns true
        every { groupService.findByGroupName("updated topic name") } returns null
        every { groupService.update(group, any()) } returns updatedGroup
        every { groupService.hasUserFollowedTopic(updatedGroup, user) } returns true

        mockMvc.perform(
            multipart(buildTopicApiUrl(topicId))
                .part(MockPart("topicName", updatedGroup.groupName.toByteArray()))
                .part(MockPart("description", updatedGroup.description.toByteArray()))
                .file(MockMultipartFile("profileImage", "image.jpeg", "image/jpeg", "some image".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it })
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.topicName").value(updatedGroup.groupName))
            .andExpect(jsonPath("$.data.description").value(updatedGroup.description))
            .andExpect { jsonPath("$.data.createdAt").exists() }
            .andExpect { jsonPath("$.data.createdAt").exists() }
            .andExpect(jsonPath("$.error.reason").doesNotExist())
    }

    @Test
    fun `should return 400 when updating topic name is not unique`() {
        val topicId = group.id!!
        val groupUpdate = GroupUpdate(
            groupName = "duplicate topic name",
            description = "updated description"
        )

        // Mocking the necessary service responses
        every { groupService.find(topicId) } returns group
        every { userService.findCurrentUser() } returns user
        every { groupService.isUserTopicOwner(group, user) } returns true
        every { groupService.findByGroupName("duplicate topic name") } returns group // Duplicate topic name found

        mockMvc.perform(
            multipart(buildTopicApiUrl(topicId))
                .part(MockPart("topicName", groupUpdate.groupName!!.toByteArray()))
                .part(MockPart("description", groupUpdate.description!!.toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "PUT"; it })
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.details").value("topic name must be unique: ${groupUpdate.groupName}"))
    }

    @Test
    fun `should return 404 when updating non-existing topic`() {
        val topicId = "invalid-id"
        every { groupService.find(topicId) } returns null
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
        val topicId = group.id!!

        every { groupService.find(topicId) } returns group
        every { userService.findCurrentUser() } returns user
        every { groupService.isUserTopicOwner(group, user) } returns true
        every { groupService.delete(group) } returns Unit

        mockMvc.delete(buildTopicApiUrl(topicId))
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.error.reason").doesNotExist() } // Ensure no error is present
            .andExpect { jsonPath("$.data").exists() }
    }

    @Test
    fun `should return 404 when deleting non-existing topic`() {
        val topicId = "invalid-id"
        every { groupService.find(topicId) } returns null
        mockMvc.delete(buildTopicApiUrl(topicId))
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `should return 403 when creating post if user is not following the topic`() {
        val topicId = group.id!!

        every { groupService.find(topicId) } returns group
        every { groupService.hasUserFollowedTopic(group, user) } returns false

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
        val topicId = group.id!!
        val postId = "post-id"
        val post = Fixture.createPostFixture(group = group, author = user)

        every { groupService.find(topicId) } returns group
        every { postService.find(postId) } returns post
        every { postService.isPostAuthor(post, user) } returns false
        every { groupService.isUserTopicOwner(group, user) } returns true
        every { postService.delete(post) } returns Unit

        mockMvc.delete(buildTopicApiUrl(topicId, "posts", postId))
            .andExpect { status { isOk() } }
    }

    @Test
    fun `should delete post if user is post author`() {
        val topicId = group.id!!
        val postId = "post-id"
        val post = Fixture.createPostFixture(group = group, author = user)

        every { groupService.find(topicId) } returns group
        every { postService.find(postId) } returns post
        every { postService.isPostAuthor(post, user) } returns true
        every { groupService.isUserTopicOwner(group, user) } returns false
        every { postService.delete(post) } returns Unit

        mockMvc.delete(buildTopicApiUrl(topicId, "posts", postId))
            .andExpect { status { isOk() } }
    }

    @Test
    fun `should return 403 when deleting post if user is neither post author nor topic owner`() {
        val topicId = group.id!!
        val postId = "post-id"
        val post = Fixture.createPostFixture(group = group, author = user)

        every { groupService.find(topicId) } returns group
        every { postService.find(postId) } returns post
        every { postService.isPostAuthor(post, user) } returns false
        every { groupService.isUserTopicOwner(group, user) } returns false

        mockMvc.delete(buildTopicApiUrl(topicId, "posts", postId))
            .andExpect { status { isForbidden() } }
            .andExpect { jsonPath("$.error").value(ErrorCode.USER_ACTION_DENIED.name) }
            .andExpect { jsonPath("$.details").value("user is not the author of this post or the topic owner") }
    }

    @Test
    fun `should return 404 when deleting non-existing post in topic`() {
        val topicId = group.id!!
        val invalidPostId = "invalid-post-id"

        every { groupService.find(topicId) } returns group
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
            Fixture.createTopicAggregateFixture(group),
            Fixture.createTopicAggregateFixture(Fixture.createTopicFixture(user))
        )

        val nextPageToken = PageToken(
            cursors = mapOf("createdAt" to CursorValue.from(Instant.now()))
        )

        val paginationSlice = PaginationSlice(
            items = topics,
            nextPageToken = nextPageToken
        )

        every { groupService.findAll(any()) } returns paginationSlice

        mockMvc.get(buildTopicApiUrl(params = paginationParams))
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                header { string(PaginationSlice.HEADER_PAGE_TOKEN, nextPageToken.encode()) }
                header { string(PaginationSlice.HEADER_PAGE_SIZE, topics.size.toString()) }
                jsonPath("$.data.length()") { value(topics.size) }
                jsonPath("$.data[0].id") { value(topics[0].topicId) }
                jsonPath("$.data[0].topicName") { value(topics[0].group.groupName) }
            }
    }

    @Test
    fun `should return a list of users following a topic with pagination`() {
        val topicId = group.id!!
        val pageToken = PageToken.create()
        val limit = 10
        val paginationParams = mapOf(
            PaginationRequest.PAGE_TOKEN_PARAM to pageToken.encode(),
            PaginationRequest.PAGE_SIZE_PARAM to limit.toString()
        )

        // Create a list of users following the topic
        val users = listOf(
            Fixture.createUserFixture(),
            Fixture.createUserFixture(),
            Fixture.createUserFixture()
        )

        val nextPageToken = PageToken(
            cursors = mapOf("username" to CursorValue.from(users.last().username))
        )

        val paginationSlice = PaginationSlice(
            items = users,
            nextPageToken = nextPageToken
        )

        // Mock the service call
        every { groupService.find(topicId) } returns group
        every { userService.getAllUsersFollowingTopic(topicId, any()) } returns paginationSlice

        mockMvc.get(buildTopicApiUrl(topicId, "users", params = paginationParams))
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                header { string(PaginationSlice.HEADER_PAGE_TOKEN, nextPageToken.encode()) }
                header { string(PaginationSlice.HEADER_PAGE_SIZE, users.size.toString()) }
                jsonPath("$.data.length()") { value(users.size) }
                // Verify first user in the list
                jsonPath("$.data[0].id") { value(users[0].id) }
                jsonPath("$.data[0].username") { value(users[0].username) }
                // Verify that sensitive information is not included
                jsonPath("$.data[0].email").doesNotExist()
                jsonPath("$.data[0].idToken").doesNotExist()
            }
    }

    @Test
    fun `should return 404 when getting users for non-existing topic`() {
        val invalidTopicId = "invalid-topic-id"
        val paginationParams = mapOf(
            PaginationRequest.PAGE_TOKEN_PARAM to PageToken.create().encode(),
            PaginationRequest.PAGE_SIZE_PARAM to "10"
        )

        every { groupService.find(invalidTopicId) } returns null

        mockMvc.get(buildTopicApiUrl(invalidTopicId, "users", params = paginationParams))
            .andExpect {
                status { isNotFound() }
                content { contentType(MediaType.APPLICATION_JSON) }
            }
    }
}
