package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.`test-util`.Fixture
import com.kogo.content.service.PostService
import com.kogo.content.service.UserService
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.Post
import com.kogo.content.storage.model.entity.User
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class FeedControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean private lateinit var postService: PostService
    @MockkBean private lateinit var userService: UserService

    private lateinit var currentUser: User
    private lateinit var group: Group
    private lateinit var post: Post

    @BeforeEach
    fun setup() {
        currentUser = Fixture.createUserFixture()
        group = Fixture.createGroupFixture(owner = currentUser)
        post = Fixture.createPostFixture(
            group = group,
            author = currentUser,
            likes = mutableListOf(),
            viewerIds = mutableListOf()
        )

        every { userService.findCurrentUser() } returns currentUser
    }

    @Test
    fun `should get trending posts successfully`() {
        val posts = listOf(post)
        val paginationSlice = PaginationSlice(items = posts)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { postService.findAllTrending(capture(paginationRequestSlot)) } returns paginationSlice

        mockMvc.get("/media/feeds/trending") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(post.id) }
            jsonPath("$.data[0].author.id") { value(currentUser.id) }
            jsonPath("$.data[0].groupId") { value(group.id) }
            jsonPath("$.data[0].likeCount") { value(post.activeLikes.size) }
            jsonPath("$.data[0].viewCount") { value(post.viewerIds.size) }
        }

        verify { postService.findAllTrending(any()) }
    }

    @Test
    fun `should get latest posts in following groups successfully`() {
        val posts = listOf(post)
        val paginationSlice = PaginationSlice(items = posts)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { postService.findAllInFollowing(capture(paginationRequestSlot), currentUser) } returns paginationSlice

        mockMvc.get("/media/feeds/latestInFollowing") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(post.id) }
            jsonPath("$.data[0].author.id") { value(currentUser.id) }
            jsonPath("$.data[0].groupId") { value(group.id) }
            jsonPath("$.data[0].likeCount") { value(post.activeLikes.size) }
            jsonPath("$.data[0].viewCount") { value(post.viewerIds.size) }
        }

        verify { postService.findAllInFollowing(any(), currentUser) }
    }

    @Test
    fun `should handle pagination parameters correctly`() {
        val paginationRequestSlot = slot<PaginationRequest>()
        every { postService.findAllTrending(capture(paginationRequestSlot)) } returns PaginationSlice(items = emptyList())

        mockMvc.get("/media/feeds/trending?limit=5") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }

        verify { postService.findAllTrending(match { it.limit == 5 }) }
    }
}

