package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.test.util.Fixture
import com.kogo.content.service.GroupService
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
    @MockkBean private lateinit var groupService: GroupService

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

        every { postService.findAllTrending(capture(paginationRequestSlot), any()) } returns paginationSlice

        mockMvc.get("/media/feeds/trendingPosts") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(post.id) }
            jsonPath("$.data[0].author.id") { value(currentUser.id) }
            jsonPath("$.data[0].likeCount") { value(post.activeLikes.size) }
            jsonPath("$.data[0].viewCount") { value(post.viewerIds.size) }
        }

        verify { postService.findAllTrending(any(), any()) }
    }

    @Test
    fun `should get latest posts in following groups successfully`() {
        val posts = listOf(post)
        val paginationSlice = PaginationSlice(items = posts)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { postService.findAllLatestInFollowing(capture(paginationRequestSlot), currentUser) } returns paginationSlice

        mockMvc.get("/media/feeds/latestPosts") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(post.id) }
            jsonPath("$.data[0].author.id") { value(currentUser.id) }
            jsonPath("$.data[0].likeCount") { value(post.activeLikes.size) }
            jsonPath("$.data[0].viewCount") { value(post.viewerIds.size) }
        }

        verify { postService.findAllLatestInFollowing(any(), currentUser) }
    }

    @Test
    fun `should handle pagination parameters correctly`() {
        val paginationRequestSlot = slot<PaginationRequest>()
        every { postService.findAllTrending(capture(paginationRequestSlot), any()) } returns PaginationSlice(items = emptyList())

        mockMvc.get("/media/feeds/trendingPosts?limit=5") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }

        verify { postService.findAllTrending(match { it.limit == 5 }, any()) }
    }

    @Test
    fun `should get trending groups successfully`() {
        val groups = listOf(group)
        val paginationSlice = PaginationSlice(items = groups)
        val paginationRequestSlot = slot<PaginationRequest>()

        every { groupService.findAllTrending(capture(paginationRequestSlot)) } returns paginationSlice

        mockMvc.get("/media/feeds/trendingGroups") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(group.id) }
            jsonPath("$.data[0].groupName") { value(group.groupName) }
            jsonPath("$.data[0].description") { value(group.description) }
            jsonPath("$.data[0].owner.id") { value(currentUser.id) }
            jsonPath("$.data[0].followerCount") { value(group.followers.size) }
        }

        verify { groupService.findAllTrending(any()) }
    }

    @Test
    fun `should handle pagination parameters for trending groups`() {
        val paginationRequestSlot = slot<PaginationRequest>()
        every { groupService.findAllTrending(capture(paginationRequestSlot)) } returns PaginationSlice(items = emptyList())

        mockMvc.get("/media/feeds/trendingGroups?limit=5") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }

        verify { groupService.findAllTrending(match { it.limit == 5 }) }
    }
}

