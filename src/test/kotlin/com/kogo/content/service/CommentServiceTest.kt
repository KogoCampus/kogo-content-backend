package com.kogo.content.service

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.SortDirection
import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.User
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.LikeRepository
import com.kogo.content.storage.entity.Like
import com.kogo.content.storage.repository.ViewerRepository
import com.kogo.content.storage.view.CommentAggregate
import com.kogo.content.storage.view.CommentAggregateView
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class CommentServiceTest {
    private val commentRepository: CommentRepository = mockk()
    private val likeRepository: LikeRepository = mockk()
    private val commentAggregateView: CommentAggregateView = mockk()
    private val viewRepository: ViewerRepository = mockk()

    private val commentService = CommentService(
        commentRepository = commentRepository,
        likeRepository = likeRepository,
        commentAggregateView = commentAggregateView,
        viewerRepository = viewRepository,
    )

    @Test
    fun `should create new comment and refresh aggregate view`() {
        val post = mockk<Post> { every { id } returns "test-post-id" }
        val author = mockk<User> { every { id } returns "test-user-id" }
        val commentDto = CommentDto(content = "Test comment")
        val savedComment = Comment(
            id = "test-comment-id",
            content = commentDto.content,
            post = post,
            author = author,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { commentRepository.save(any()) } returns savedComment
        every { commentAggregateView.refreshView("test-comment-id") } returns mockk<CommentAggregate>()

        val result = commentService.create(post, author, commentDto)

        assertThat(result).isEqualTo(savedComment)
        verify {
            commentRepository.save(match {
                it.content == commentDto.content &&
                it.post == post &&
                it.author == author
            })
            commentAggregateView.refreshView("test-comment-id")
        }
    }

    @Test
    fun `should update existing comment and refresh aggregate view`() {
        val comment = Comment(
            id = "test-comment-id",
            content = "Original content",
            post = mockk(),
            author = mockk(),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600)
        )
        val update = CommentUpdate(content = "Updated content")

        every { commentRepository.save(any()) } answers { firstArg() }
        every { commentAggregateView.refreshView("test-comment-id") } returns mockk<CommentAggregate>()

        val result = commentService.update(comment, update)

        assertThat(result.content).isEqualTo(update.content)
        assertThat(result.updatedAt).isAfter(comment.createdAt)
        verify {
            commentRepository.save(match {
                it.id == comment.id &&
                it.content == update.content
            })
            commentAggregateView.refreshView("test-comment-id")
        }
    }

    @Test
    fun `should find comments by post with pagination`() {
        val post = mockk<Post> { every { id } returns "test-post-id" }
        val paginationRequest = PaginationRequest(limit = 10)
        val expectedRequest = paginationRequest
            .withFilter("post", "test-post-id")
            .withSort("createdAt", SortDirection.DESC)

        every { commentAggregateView.findAll(expectedRequest) } returns mockk()

        commentService.findAggregatesByPost(post, paginationRequest)

        verify { commentAggregateView.findAll(expectedRequest) }
    }

    @Test
    fun `should handle add like operations correctly`() {
        val comment = mockk<Comment> { every { id } returns "test-comment-id" }
        val user = mockk<User> { every { id } returns "test-user-id" }

        // Test successful like operation
        val like = mockk<Like>()
        every { likeRepository.addLike("test-comment-id", "test-user-id") } returns like
        every { commentAggregateView.refreshView("test-comment-id") } returns mockk<CommentAggregate>()

        commentService.addLike(comment, user)
        verify {
            likeRepository.addLike("test-comment-id", "test-user-id")
            commentAggregateView.refreshView("test-comment-id")
        }

        // Test unsuccessful like operation (already liked)
        every { likeRepository.addLike("test-comment-id", "test-user-id") } returns null

        commentService.addLike(comment, user)
        verify(exactly = 1) { commentAggregateView.refreshView("test-comment-id") } // Should not be called again
    }

    @Test
    fun `should handle remove like operations correctly`() {
        val comment = mockk<Comment> { every { id } returns "test-comment-id" }
        val user = mockk<User> { every { id } returns "test-user-id" }

        // Test successful unlike operation
        every { likeRepository.removeLike("test-comment-id", "test-user-id") } returns true
        every { commentAggregateView.refreshView("test-comment-id") } returns mockk<CommentAggregate>()

        commentService.removeLike(comment, user)
        verify {
            likeRepository.removeLike("test-comment-id", "test-user-id")
            commentAggregateView.refreshView("test-comment-id")
        }

        // Test unsuccessful unlike operation (not liked)
        every { likeRepository.removeLike("test-comment-id", "test-user-id") } returns false

        commentService.removeLike(comment, user)
        verify(exactly = 1) { commentAggregateView.refreshView("test-comment-id") } // Should not be called again
    }
}

