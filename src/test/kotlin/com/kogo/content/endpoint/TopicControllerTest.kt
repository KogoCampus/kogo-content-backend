package com.kogo.content.endpoint

import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.service.entity.UserContextService
import com.kogo.content.service.entity.TopicService
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.entity.Topic
import com.kogo.content.endpoint.`test-util`.Fixture
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
    lateinit var userService: UserContextService

    lateinit var user: UserDetails
    lateinit var topic: Topic

    @BeforeEach
    fun setup() {
        user = Fixture.createUserFixture()
        topic = Fixture.createTopicFixture(user)
    }

    @Test
    fun `should return a topic by id`() {
        val topicId = topic.id!!
        every { topicService.find(topicId) } returns topic
        mockMvc.get(buildTopicApiUrl(topicId))
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.id") { value(topicId) } }
            .andExpect { jsonPath("$.data.topicName") { value(topic.topicName) } }
            .andExpect { jsonPath("$.data.createdAt").exists() }
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
    fun `should create a new topic`() {
        every { topicService.isTopicExist(any()) } returns false
        every { userService.getCurrentUserDetails() } returns user
        every { topicService.create(any(), user) } returns topic
        mockMvc.perform(
            multipart(buildTopicApiUrl())
                .part(MockPart("topicName", topic.topicName.toByteArray()))
                .part(MockPart("description", topic.description.toByteArray()))
                .part(MockPart("tags", topic.tags.first().toByteArray()))
                .file(MockMultipartFile("profileImage", "image.jpeg", "image/jpeg", "some image".toByteArray()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with { it.method = "POST"; it })
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.topicName").value(topic.topicName))
            .andExpect(jsonPath("$.data.description").value(topic.description))
            .andExpect { jsonPath("$.data.createdAt").exists() }
    }

    @Test
    fun `should return 400 when topic name is not unique`() {
        val topicName = "existing-topic"
        every { topicService.isTopicExist(topicName) } returns true
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

        every { topicService.find(topicId) } returns topic
        every { userService.getCurrentUserDetails() } returns user
        every { topicService.isTopicOwner(topic, user) } returns true
        every { topicService.isTopicExist("updated topic name") } returns false
        every { topicService.update(topic, any()) } returns updatedTopic
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
        every { userService.getCurrentUserDetails() } returns user
        every { topicService.isTopicOwner(topic, user) } returns true
        every { topicService.isTopicExist("duplicate topic name") } returns true // Duplicate topic name found

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
        every { userService.getCurrentUserDetails() } returns user
        every { topicService.isTopicOwner(topic, user) } returns true
        every { topicService.delete(topic) } returns Unit

        mockMvc.delete(buildTopicApiUrl(topicId))
            .andExpect { status { isOk() } }
            .andExpect{ jsonPath("$.error.reason").doesNotExist() } // Ensure no error is present
            .andExpect{ jsonPath("$.data").exists() }
    }

    @Test
    fun `should return 404 when deleting non-existing topic`() {
        val topicId = "invalid-id"
        every { topicService.find(topicId) } returns null
        mockMvc.delete(buildTopicApiUrl(topicId))
            .andExpect { status { isNotFound() } }
    }

    private fun buildTopicApiUrl(vararg paths: String) =
        if (paths.isNotEmpty()) "$TOPIC_API_BASE_URL/" + paths.joinToString("/")
        else TOPIC_API_BASE_URL
}
