package com.kogo.content.endpoint.controller

import com.kogo.content.service.UserService
import com.kogo.content.storage.model.entity.User
import com.kogo.content.endpoint.`test-util`.Fixture
import com.kogo.content.service.PostService
import com.kogo.content.service.GroupService
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.view.TopicAggregate
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable Spring Security filter chain during testing
class MeControllerTest  @Autowired constructor(
    private val mockMvc: MockMvc
) {

    companion object {
        private const val ME_API_BASE_URL = "/me"
    }

    @MockkBean
    lateinit var userService: UserService

    @MockkBean
    lateinit var postService: PostService

    @MockkBean
    lateinit var groupService: GroupService

    /**
     * Fixtures
     */
    private final val user: User = Fixture.createUserFixture()
    private final val group: Group = Fixture.createTopicFixture(user)
    private final val topicAggregate: TopicAggregate = Fixture.createTopicAggregateFixture(group)

    private fun buildMeApiUrl(vararg paths: String) =
        if (paths.isNotEmpty()) "$ME_API_BASE_URL/" + paths.joinToString("/")
        else ME_API_BASE_URL

    @BeforeEach
    fun setup() {
        every { groupService.findAggregate(group.id!!) } returns topicAggregate
    }

    @Test
    fun `should return current user's info`() {
        every { userService.findCurrentUser() } returns user
        mockMvc.get(buildMeApiUrl())
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.id") { value(user.id) } }
            .andExpect { jsonPath("$.data.username") { value(user.username) } }
            .andExpect { jsonPath("$.data.createdAt").exists() }
    }

    @Test
    fun `should return topics which user is the owner`() {
        val topics = listOf(
            Fixture.createTopicFixture(owner = user)
        )

        every { userService.findCurrentUser() } returns user
        every { groupService.findByOwner(user) } returns topics
        every { groupService.findAggregate(any()) } returns Fixture.createTopicAggregateFixture(topics[0])
        every { groupService.hasUserFollowedTopic(any(), any()) } returns true

        mockMvc.get("$ME_API_BASE_URL/ownership/topics")
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data[0].id") { value(topics[0].id) } }
            .andExpect { jsonPath("$.data[0].createdAt").exists() }
            .andExpect { jsonPath("$.data[0].updatedAt").exists() }
    }
}
