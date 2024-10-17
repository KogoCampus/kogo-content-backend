package com.kogo.content.endpoint

import com.kogo.content.endpoint.model.UserUpdate
import com.kogo.content.service.UserContextService
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable Spring Security filter chain during testing
class MeControllerTest  @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @MockkBean
    lateinit var userService: UserContextService

    companion object {
        private const val ME_API_BASE_URL = "/me"
    }

    @Test
    fun `should return current user's info`() {
        val user = createUserFixture()
        every { userService.getCurrentUserDetails() } returns user
        mockMvc.get(buildMeApiUrl())
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.id") { value(user.id) } }
            .andExpect { jsonPath("$.data.username") { value(user.username) } }
            .andExpect { jsonPath("$.data.createdAt").exists() }
    }

//    @Test
//    fun `should update current user's info`() {
//        // Create user and update fixture
//        val user = fixture<UserDetails>()
//        val userUpdate = fixture<UserUpdate>()
//
//        // Mock the service methods to return the appropriate values
//        every { userService.getCurrentUserDetails() } returns user
//        every { userService.updateUserProfile(user, userUpdate) } returns user.copy(
//            username = userUpdate.username ?: user.username,
//            profileImage = userUpdate.profileImage
//        )
//
//        mockMvc.perform(
//            multipart(buildMeApiUrl())
//                .part(MockPart("username", userUpdate.username?.toByteArray() ?: byteArrayOf()))
//                .file(MockMultipartFile("profileImage", "image.jpeg", "image/jpeg", "some image".toByteArray()))
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .with { it.method = "PUT"; it }
//        )
//            .andExpect(status().isOk)
//            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//            .andExpect(jsonPath("$.data.username").value(userUpdate.username ?: user.username))
//    }

//    @Test
//    fun `should return current user's posts`() {
//        val user = fixture<UserDetails>()
//        val posts = listOf(createPostFixture()) // Provide the topic in the fixture
//
//        every { userService.getCurrentUserDetails() } returns user
//        every { userService.getUserPosts(user) } returns posts
//
//        mockMvc.get("$ME_API_BASE_URL/ownership/posts")
//            .andExpect { status { isOk() } }
//            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
//            .andExpect { jsonPath("$.data[0].id") { value(posts[0].id) } }
//    }

    @Test
    fun `should return current user's topics`() {
        val user = createUserFixture()
        val topics = listOf(createTopicFixture())
        every { userService.getCurrentUserDetails() } returns user
        every { userService.getUserTopics(user) } returns topics

        mockMvc.get("$ME_API_BASE_URL/ownership/topics")
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data[0].id") { value(topics[0].id) } }
            .andExpect { jsonPath("$.data[0].createdAt").exists() }
    }

    private fun buildMeApiUrl(vararg paths: String) =
        if (paths.isNotEmpty()) "$ME_API_BASE_URL/" + paths.joinToString("/")
        else ME_API_BASE_URL

    private fun createUserFixture() = fixture<UserDetails>()

    private fun createPostFixture() = fixture<Post> {
        mapOf(
            "author" to createUserFixture(),
            "createdAt" to Instant.now()
        )
    }

    private fun createTopicFixture() = fixture<Topic> {
        mapOf(
            "owner" to createUserFixture(),
            "createdAt" to Instant.now()
        )
    }
}
