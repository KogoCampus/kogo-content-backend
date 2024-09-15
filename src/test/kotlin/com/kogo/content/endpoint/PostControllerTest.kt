package com.kogo.content.endpoint

import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.service.UserContextService
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.util.fixture
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
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

    @Test
    fun `should return a list of posts by topic id`() {
        val topic = createTopicFixture()
        val posts = listOf(createPostFixture(topic), createPostFixture(topic))
        val topicId = topic.id!!
        every { topicService.find(topicId) } returns topic
        every { postService.listPostsByTopicId(topicId) } returns posts
        mockMvc.get(buildPostApiUrl(topicId))
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.length()") { value(posts.size) } }
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
    fun `should create a new post`() {
        val topic = createTopicFixture()
        val post = createPostFixture(topic)
        val user = createUserFixture()
        val topicId = topic.id!!
        every { topicService.find(topicId) } returns topic
        every { userService.getCurrentUserContext() } returns user
        every { postService.create(topic, user, any())} returns post
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
    fun `should update an existing post`() {
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val post = createPostFixture(topic)
        val postId = post.id!!
        val updatedPost = Post(
            id = postId,
            title = "updated post title",
            content = "updated post content",
            topic = post.topic,
            author = post.author
        )
        every { topicService.find(postId) } returns topic
        every { postService.find(postId) } returns post
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
    fun `should delete a post`() {
        val topic = createTopicFixture()
        val topicId = topic.id!!
        val post = createPostFixture(topic)
        val postId = post.id!!
        every { topicService.find(topicId) } returns topic
        every { postService.find(postId) } returns post
        every { postService.delete(post) } returns Unit
        mockMvc.delete(buildPostApiUrl(topicId, postId))
            .andExpect { status { isOk() } }
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

    private fun buildPostApiUrl(topicId: String, vararg paths: String): String {
        val baseUrl = "/media/topics/$topicId/posts"
        return if (paths.isNotEmpty()) "${baseUrl}/" + paths.joinToString("/")
            else baseUrl
    }

    private fun createTopicFixture() = fixture<Topic> { mapOf(
        "owner" to createUserFixture()
    ) }

    private fun createPostFixture(topic: Topic) = fixture<Post> {
        mapOf(
            "topic" to topic,
            "author" to createUserFixture(),
            "comments" to emptyList<Any>(),
            "attachments" to emptyList<Any>(),
        )
    }

    private fun createUserFixture() = fixture<UserDetails>()
}
