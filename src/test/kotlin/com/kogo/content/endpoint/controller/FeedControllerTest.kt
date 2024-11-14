package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.`test-util`.Fixture
import com.kogo.content.lib.*
import com.kogo.content.service.PostService
import com.kogo.content.service.UserService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class FeedControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean
    lateinit var postService: PostService

    @MockkBean
    lateinit var userService: UserService

    private val user = Fixture.createUserFixture()
    private val topic = Fixture.createTopicFixture()
    private val posts = listOf(
        Fixture.createPostFixture(topic, user),
        Fixture.createPostFixture(topic, user)
    )
    private val postStats = posts.map { Fixture.createPostAggregateFixture(it) }

    private fun buildFeedApiUrl(feedType: String, params: Map<String, String> = emptyMap()): String {
        val baseUrl = "/media/feeds/$feedType"
        val paramBuilder = StringBuilder()
        params.forEach { paramBuilder.append("${it.key}=${it.value}&") }
        return if (paramBuilder.isNotEmpty()) "$baseUrl?$paramBuilder" else baseUrl
    }

    @BeforeEach
    fun setup() {
        every { userService.getCurrentUser() } returns user
        posts.forEachIndexed { index, post ->
            every { postService.findAggregate(post.id!!) } returns postStats[index]
            every { postService.hasUserLikedPost(post, user) } returns false
            every { postService.hasUserViewedPost(post, user) } returns false
        }
    }

    @Test
    fun `should return latest posts with pagination metadata`() {
        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken.create())
        val nextPageToken = paginationRequest.pageToken.nextPageToken(mapOf("id" to CursorValue("sample-next-page-token", CursorValueType.STRING)))
        val postAggregates = posts.map { p -> Fixture.createPostAggregateFixture(p) }
        val paginationSlice = PaginationSlice(postAggregates, nextPageToken)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { postService.findPostAggregatesByLatest(capture(paginationRequestSlot)) } returns paginationSlice

        mockMvc.get(buildFeedApiUrl("latest", mapOf("limit" to "${paginationRequest.limit}")))
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.length()") { value(posts.size) } }
            .andExpect { jsonPath("$.data[0].id") { value(posts[0].id) } }
            .andExpect { jsonPath("$.data[0].likedByCurrentUser") { value(false) } }
            .andExpect { header { string(PaginationSlice.HEADER_PAGE_TOKEN, nextPageToken.encode()) } }
            .andExpect { header { string(PaginationSlice.HEADER_PAGE_SIZE, "${paginationRequest.limit}") } }

        val capturedPaginationRequest = paginationRequestSlot.captured
        assertThat(capturedPaginationRequest.limit).isEqualTo(paginationRequest.limit)
        assertThat(capturedPaginationRequest.pageToken.toString()).isEqualTo(paginationRequest.pageToken.toString())
    }

    @Test
    fun `should return trending posts with pagination metadata`() {
        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken.create())
        val nextPageToken = paginationRequest.pageToken.nextPageToken(mapOf("id" to CursorValue("sample-next-page-token", CursorValueType.STRING)))
        val postAggregates = posts.map { p -> Fixture.createPostAggregateFixture(p) }
        val paginationSlice = PaginationSlice(postAggregates, nextPageToken)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { postService.findPostAggregatesByPopularity(capture(paginationRequestSlot)) } returns paginationSlice

        mockMvc.get(buildFeedApiUrl("trending", mapOf("limit" to "${paginationRequest.limit}")))
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.length()") { value(posts.size) } }
            .andExpect { jsonPath("$.data[0].id") { value(posts[0].id) } }
            .andExpect { jsonPath("$.data[0].likedByCurrentUser") { value(false) } }
            .andExpect { header { string(PaginationSlice.HEADER_PAGE_TOKEN, nextPageToken.encode()) } }
            .andExpect { header { string(PaginationSlice.HEADER_PAGE_SIZE, "${paginationRequest.limit}") } }

        val capturedPaginationRequest = paginationRequestSlot.captured
        assertThat(capturedPaginationRequest.limit).isEqualTo(paginationRequest.limit)
        assertThat(capturedPaginationRequest.pageToken.toString()).isEqualTo(paginationRequest.pageToken.toString())
    }
}

