package com.kogo.content.service

import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.lib.*
import com.kogo.content.search.SearchIndex
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.LikeRepository
import com.kogo.content.storage.repository.PostRepository
import com.kogo.content.storage.repository.ViewerRepository
import com.kogo.content.storage.view.PostAggregate
import com.kogo.content.storage.view.PostAggregateView
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class PostServiceTest {
    private val postRepository: PostRepository = mockk()
    private val attachmentRepository: AttachmentRepository = mockk()
    private val likeRepository: LikeRepository = mockk()
    private val viewerRepository: ViewerRepository = mockk()
    private val postAggregateView: PostAggregateView = mockk()
    private val postAggregateSearchIndex: SearchIndex<PostAggregate> = mockk()

    private val postService = PostService(
        postRepository = postRepository,
        attachmentRepository = attachmentRepository,
        likeRepository = likeRepository,
        viewerRepository = viewerRepository,
        postAggregateView = postAggregateView,
        postAggregateSearchIndex = postAggregateSearchIndex
    )

    @Test
    fun `should create new post and refresh aggregate view`() {
        val topic = mockk<Topic> { every { id } returns "test-topic-id" }
        val author = mockk<User> { every { id } returns "test-user-id" }
        val postDto = PostDto(
            title = "Test title",
            content = "Test content",
            images = listOf(mockk()),
            videos = emptyList()
        )
        val savedPost = Post(
            id = "test-post-id",
            title = postDto.title,
            content = postDto.content,
            topic = topic,
            author = author,
            attachments = listOf(mockk()),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { attachmentRepository.saveFiles(any()) } returns listOf(mockk())
        every { postRepository.save(any()) } returns savedPost
        every { postAggregateView.refreshView("test-post-id") } returns mockk<PostAggregate>()

        val result = postService.create(topic, author, postDto)

        assertThat(result).isEqualTo(savedPost)
        verify {
            postRepository.save(match {
                it.title == postDto.title &&
                it.content == postDto.content &&
                it.topic == topic &&
                it.author == author
            })
            postAggregateView.refreshView("test-post-id")
        }
    }

    @Test
    fun `should update existing post and refresh aggregate view`() {
        val post = Post(
            id = "test-post-id",
            title = "Original title",
            content = "Original content",
            topic = mockk(),
            author = mockk(),
            attachments = listOf(mockk { every { id } returns "att-1" }),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600)
        )
        val update = PostUpdate(
            title = "Updated title",
            content = "Updated content",
            images = listOf(mockk()),
            videos = emptyList(),
            attachmentDelete = listOf("att-1")
        )

        every { attachmentRepository.saveFiles(any()) } returns listOf(mockk())
        every { attachmentRepository.delete(any()) } just runs
        every { postRepository.save(any()) } answers { firstArg() }
        every { postAggregateView.refreshView("test-post-id") } returns mockk<PostAggregate>()

        val result = postService.update(post, update)

        assertThat(result.title).isEqualTo(update.title)
        assertThat(result.content).isEqualTo(update.content)
        assertThat(result.updatedAt).isAfter(post.createdAt)
        verify {
            postRepository.save(match {
                it.id == post.id &&
                it.title == update.title &&
                it.content == update.content
            })
            postAggregateView.refreshView("test-post-id")
            attachmentRepository.delete(any())
        }
    }

    @Test
    fun `should find posts by topic with pagination`() {
        val topic = mockk<Topic> { every { id } returns "test-topic-id" }
        val paginationRequest = PaginationRequest(limit = 10)
        val expectedRequest = paginationRequest.withFilter("topic", topic.id!!)

        every { postAggregateView.findAll(expectedRequest) } returns mockk()

        postService.findPostsByTopic(topic, paginationRequest)

        verify { postAggregateView.findAll(expectedRequest) }
    }

    @Test
    fun `should handle search operations`() {
        val searchText = "test"
        val paginationRequest = PaginationRequest(limit = 10)

        every {
            postAggregateSearchIndex.search(
                searchText = searchText,
                paginationRequest = paginationRequest,
                boost = any()
            )
        } returns mockk()

        // Test regular search
        postService.searchPostAggregatesByKeyword(searchText, paginationRequest)
        verify { postAggregateSearchIndex.search(searchText, paginationRequest, 1.0) }

        // Test popularity-boosted search
        postService.searchPostAggregatesByKeywordAndPopularity(searchText, paginationRequest)
        verify { postAggregateSearchIndex.search(searchText, paginationRequest, 2.0) }
    }

    @Test
    fun `should handle interaction operations`() {
        val post = mockk<Post> { every { id } returns "test-post-id" }
        val user = mockk<User> { every { id } returns "test-user-id" }

        // Test like operations
        every { likeRepository.addLike("test-post-id", "test-user-id") } returns mockk()
        every { postAggregateView.refreshView("test-post-id") } returns mockk<PostAggregate>()
        postService.addLike(post, user)
        verify {
            likeRepository.addLike("test-post-id", "test-user-id")
            postAggregateView.refreshView("test-post-id")
        }

        // Test view operations
        every { viewerRepository.addView("test-post-id", "test-user-id") } returns mockk()
        postService.addViewer(post, user)
        verify {
            viewerRepository.addView("test-post-id", "test-user-id")
            postAggregateView.refreshView("test-post-id")
        }
    }

    @Test
    fun `should delete post and its attachments`() {
        val post = mockk<Post> {
            every { id } returns "test-post-id"
            every { attachments } returns listOf(mockk(), mockk())
        }

        every { attachmentRepository.delete(any()) } just runs
        every { postRepository.deleteById(any()) } just runs

        postService.delete(post)

        verify(exactly = 2) { attachmentRepository.delete(any()) }
        verify { postRepository.deleteById("test-post-id") }
    }
}
