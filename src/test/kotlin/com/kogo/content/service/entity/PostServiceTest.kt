package com.kogo.content.service.entity

import com.kogo.content.service.PostService
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.service.pagination.PageToken
import com.kogo.content.service.search.SearchService
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.PostRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant

class PostServiceTest {
    private val postRepository: PostRepository = mockk()
    private val attachmentRepository: AttachmentRepository = mockk()
    private val fileHandler: FileHandler = mockk()
    private val postSearchService: SearchService<Post> = mockk()

    private val postService: PostService = PostService(
        postRepository = postRepository,
        attachmentRepository = attachmentRepository,
        fileHandler = fileHandler,
        postSearchService = postSearchService
    )

    @Test
    fun `should find post by id`() {
        val postId = "test-post-id"
        val expectedPost = mockk<Post>()

        every { postRepository.findByIdOrNull(postId) } returns expectedPost

        val result = postService.find(postId)

        assertThat(result).isEqualTo(expectedPost)
        verify { postRepository.findByIdOrNull(postId) }
    }

    @Test
    fun `should return a paginated list of posts by topic id`() {
        val topicId = "test-topic-id"
        val post1 = mockk<Post> { every { id } returns "sample-post-id-1" }
        val post2 = mockk<Post> { every { id } returns "sample-post-id-2" }
        val posts = listOf(post1, post2)

        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken())
        val pageable = PageRequest.of(0, paginationRequest.limit, Sort.by(Sort.Direction.DESC, "_id"))

        every { postRepository.findAllByTopicId(topicId, pageable) } returns posts

        val result = postService.listPostsByTopicId(topicId, paginationRequest)

        assertThat(result.items).hasSize(2)
        assertThat(result.nextPage!!.pageLastResourceId).isEqualTo("sample-post-id-2")
        verify { postRepository.findAllByTopicId(topicId, pageable) }
    }

    @Test
    fun `should return posts by author id`() {
        val authorId = "test-author-id"
        val expectedPosts = listOf(mockk<Post>(), mockk<Post>())

        every { postRepository.findAllByAuthorId(authorId) } returns expectedPosts

        val result = postService.listPostsByAuthorId(authorId)

        assertThat(result).isEqualTo(expectedPosts)
        verify { postRepository.findAllByAuthorId(authorId) }
    }

    @Test
    fun `should delete post and its attachments`() {
        val post = mockk<Post> {
            every { id } returns "test-post-id"
            every { attachments } returns listOf(mockk(), mockk())
        }

        every { attachmentRepository.delete(any()) } just Runs
        every { postRepository.deleteById(any()) } just Runs

        postService.delete(post)

        verify(exactly = 2) { attachmentRepository.delete(any()) }
        verify { postRepository.deleteById("test-post-id") }
    }

    @Test
    fun `should create a like under the post`() {
        val user = mockk<UserDetails> { every { id } returns "test-user-id" }
        val post = Post(
            id = "test-post-id",
            title = "test title",
            content = "test content",
            author = mockk(),
            topic = mockk(),
            likes = 0,
            createdAt = Instant.now()
        )

        every { postRepository.addLike("test-post-id", "test-user-id") } returns mockk()
        every { postRepository.save(any()) } returns post

        postService.addLike(post, user)

        assertThat(post.likes).isEqualTo(1)
        verify {
            postRepository.addLike("test-post-id", "test-user-id")
            postRepository.save(post)
        }
    }

    @Test
    fun `should remove like from post`() {
        val user = mockk<UserDetails> { every { id } returns "test-user-id" }
        val post = Post(
            id = "test-post-id",
            title = "test title",
            content = "test content",
            author = mockk(),
            topic = mockk(),
            likes = 1
        )

        every { postRepository.removeLike("test-post-id", "test-user-id") } returns true
        every { postRepository.save(any()) } returns post

        postService.removeLike(post, user)

        assertThat(post.likes).isEqualTo(0)
        verify {
            postRepository.removeLike("test-post-id", "test-user-id")
            postRepository.save(post)
        }
    }

    @Test
    fun `should create a view under the post`() {
        val user = mockk<UserDetails> { every { id } returns "test-user-id" }
        val post = Post(
            id = "test-post-id",
            title = "test title",
            content = "test content",
            author = mockk(),
            topic = mockk(),
            viewCount = 0,
            createdAt = Instant.now()
        )

        every { postRepository.addViewCount("test-post-id", "test-user-id") } returns mockk()
        every { postRepository.save(any()) } returns post

        postService.addView(post, user)

        assertThat(post.viewCount).isEqualTo(1)
        verify {
            postRepository.addViewCount("test-post-id", "test-user-id")
            postRepository.save(post)
        }
    }

    @Test
    fun `should check if user is post author`() {
        val user = mockk<UserDetails>()
        val post = mockk<Post> { every { author } returns user }

        val result = postService.isPostAuthor(post, user)

        assertThat(result).isTrue()
    }

    @Test
    fun `should check if user has liked post`() {
        val postId = "test-post-id"
        val userId = "test-user-id"
        val user = mockk<UserDetails> { every { id } returns userId }
        val post = mockk<Post> { every { id } returns postId }

        every { postRepository.findLike(postId, userId) } returns mockk()

        val result = postService.hasUserLikedPost(post, user)

        assertThat(result).isTrue()
        verify { postRepository.findLike(postId, userId) }
    }
}
