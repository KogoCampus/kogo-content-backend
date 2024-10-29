package com.kogo.content.endpoint

import com.kogo.content.service.FeedService
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationResponse
import com.kogo.content.service.pagination.PageToken
import com.kogo.content.endpoint.`test-util`.Fixture
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
    lateinit var feedService: FeedService

    private val topic = Fixture.createTopicFixture()
    private val posts = listOf(
        Fixture.createPostFixture(topic, Fixture.createUserFixture()),
        Fixture.createPostFixture(topic, Fixture.createUserFixture())
    )

    private fun buildFeedApiUrl(feedType: String, params: Map<String, String> = emptyMap()): String {
        val baseUrl = "/media/feeds/$feedType"
        val paramBuilder = StringBuilder()
        params.forEach { paramBuilder.append("${it.key}=${it.value}&") }
        return if (paramBuilder.isNotEmpty()) "$baseUrl?$paramBuilder" else baseUrl
    }

    @BeforeEach
    fun setup() {
        // Setup any common mock behavior here
    }

    @Test
    fun `should return latest posts with pagination metadata`() {
        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken())
        val nextPageToken = paginationRequest.pageToken.nextPageToken("sample-next-page-token")
        val paginationResponse = PaginationResponse(posts, nextPageToken)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { feedService.listPostsByLatest(capture(paginationRequestSlot)) } returns paginationResponse

        mockMvc.get(buildFeedApiUrl("latest", mapOf("limit" to "${paginationRequest.limit}")))
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
    fun `should return trending posts with pagination metadata`() {
        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken())
        val nextPageToken = paginationRequest.pageToken.nextPageToken("sample-next-page-token")
        val paginationResponse = PaginationResponse(posts, nextPageToken)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { feedService.listPostsByPopularity(capture(paginationRequestSlot)) } returns paginationResponse

        mockMvc.get(buildFeedApiUrl("trending", mapOf("limit" to "${paginationRequest.limit}")))
            .andExpect { status { isOk() } }
            .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
            .andExpect { jsonPath("$.data.length()") { value(posts.size) } }
            .andExpect { header { string(PaginationResponse.HEADER_NAME_PAGE_TOKEN, nextPageToken.toString()) } }
            .andExpect { header { string(PaginationResponse.HEADER_NAME_PAGE_SIZE, "${paginationRequest.limit}") } }

        val capturedPaginationRequest = paginationRequestSlot.captured
        assertThat(capturedPaginationRequest.limit).isEqualTo(paginationRequest.limit)
        assertThat(capturedPaginationRequest.pageToken.toString()).isEqualTo(paginationRequest.pageToken.toString())
    }
}

