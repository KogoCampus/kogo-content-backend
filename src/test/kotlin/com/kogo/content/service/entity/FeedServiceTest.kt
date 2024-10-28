package com.kogo.content.service.entity

import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PageToken
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.repository.PostRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class FeedServiceTest {
    private val postRepository: PostRepository = mockk()
    private val feedService = FeedService(postRepository)

    @BeforeEach
    fun setup() {
        clearMocks(postRepository)
    }

    @Test
    fun `should return paginated list of latest posts`() {
        val post1 = mockk<Post> { every { id } returns "post-1" }
        val post2 = mockk<Post> { every { id } returns "post-2" }
        val posts = listOf(post1, post2)

        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken())
        val pageable = PageRequest.of(0, paginationRequest.limit, Sort.by(Sort.Direction.DESC, "_id"))

        every { postRepository.findAllByOrderByIdDesc(pageable) } returns posts

        val result = feedService.listPostsByLatest(paginationRequest)

        assertThat(result.items).hasSize(2)
        assertThat(result.nextPage?.pageLastResourceId).isEqualTo("post-2")
        verify { postRepository.findAllByOrderByIdDesc(pageable) }
    }

    @Test
    fun `should return paginated list of latest posts with page token`() {
        val post1 = mockk<Post> { every { id } returns "post-3" }
        val post2 = mockk<Post> { every { id } returns "post-4" }
        val posts = listOf(post1, post2)

        val pageToken = PageToken(pageLastResourceId = "post-2")
        val paginationRequest = PaginationRequest(limit = 2, pageToken = pageToken)
        val pageable = PageRequest.of(0, paginationRequest.limit, Sort.by(Sort.Direction.DESC, "_id"))

        every { postRepository.findAllByIdLessThanOrderByIdDesc("post-2", pageable) } returns posts

        val result = feedService.listPostsByLatest(paginationRequest)

        assertThat(result.items).hasSize(2)
        assertThat(result.nextPage?.pageLastResourceId).isEqualTo("post-4")
        verify { postRepository.findAllByIdLessThanOrderByIdDesc("post-2", pageable) }
    }

    @Test
    fun `should return paginated list of popular posts`() {
        val post1 = mockk<Post> { every { id } returns "post-1" }
        val post2 = mockk<Post> { every { id } returns "post-2" }
        val posts = listOf(post1, post2)

        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken())
        val pageable = PageRequest.of(0, paginationRequest.limit, Sort.by(Sort.Direction.DESC, "_id"))

        every { postRepository.findAllPopular(0, 2) } returns posts

        val result = feedService.listPostsByPopularity(paginationRequest)

        assertThat(result.items).hasSize(2)
        assertThat(result.nextPage?.pageLastResourceId).isEqualTo("1")
        verify { postRepository.findAllPopular(0, 2) }
    }

    @Test
    fun `should return paginated list of popular posts with page token`() {
        val post1 = mockk<Post> { every { id } returns "post-3" }
        val post2 = mockk<Post> { every { id } returns "post-4" }
        val posts = listOf(post1, post2)

        val pageToken = PageToken(pageLastResourceId = "1")
        val paginationRequest = PaginationRequest(limit = 2, pageToken = pageToken)

        every { postRepository.findAllPopular(1, 2) } returns posts

        val result = feedService.listPostsByPopularity(paginationRequest)

        assertThat(result.items).hasSize(2)
        assertThat(result.nextPage?.pageLastResourceId).isEqualTo("2")
        verify { postRepository.findAllPopular(1, 2) }
    }
}

