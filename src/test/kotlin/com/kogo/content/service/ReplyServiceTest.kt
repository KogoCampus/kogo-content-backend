package com.kogo.content.service

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.common.*
import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.entity.Like
import com.kogo.content.storage.entity.Reply
import com.kogo.content.storage.entity.User
import com.kogo.content.storage.repository.LikeRepository
import com.kogo.content.storage.repository.ReplyRepository
import com.kogo.content.storage.repository.ViewerRepository
import com.kogo.content.storage.view.ReplyAggregateView
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
class ReplyServiceTest {
    private val replyRepository: ReplyRepository = mockk()
    private val likeRepository: LikeRepository = mockk()
    private val replyAggregateView: ReplyAggregateView = mockk()
    private val viewerRepository: ViewerRepository = mockk()

    private val replyService = ReplyService(
        replyRepository = replyRepository,
        likeRepository = likeRepository,
        replyAggregateView = replyAggregateView,
        viewerRepository = viewerRepository
    )

    @Test
    fun `should create new reply and refresh aggregate view`() {
        val comment = mockk<Comment> { every { id } returns "test-comment-id" }
        val author = mockk<User> { every { id } returns "test-user-id" }
        val replyDto = CommentDto(content = "Test reply content")
        val savedReply = Reply(
            id = "test-reply-id",
            content = replyDto.content,
            comment = comment,
            author = author,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { replyRepository.save(any()) } returns savedReply
        every { replyAggregateView.refreshView("test-reply-id") } returns mockk()

        val result = replyService.create(comment, author, replyDto)

        assertThat(result).isEqualTo(savedReply)
        verify {
            replyRepository.save(match {
                it.content == replyDto.content &&
                it.comment == comment &&
                it.author == author
            })
            replyAggregateView.refreshView("test-reply-id")
        }
    }

    @Test
    fun `should update existing reply and refresh aggregate view`() {
        val reply = Reply(
            id = "test-reply-id",
            content = "Original content",
            comment = mockk(),
            author = mockk(),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(3600)
        )
        val update = CommentUpdate(content = "Updated content")

        every { replyRepository.save(any()) } answers { firstArg() }
        every { replyAggregateView.refreshView("test-reply-id") } returns mockk()

        val result = replyService.update(reply, update)

        assertThat(result.content).isEqualTo(update.content)
        assertThat(result.updatedAt).isAfter(reply.createdAt)
        verify {
            replyRepository.save(match {
                it.id == reply.id &&
                it.content == update.content
            })
            replyAggregateView.refreshView("test-reply-id")
        }
    }

    @Test
    fun `should find replies by comment with pagination`() {
        val comment = mockk<Comment> { every { id } returns "test-comment-id" }
        val paginationRequest = PaginationRequest(limit = 10)
        val expectedRequest = paginationRequest.withFilter("comment", comment.id!!)

        every { replyAggregateView.findAll(expectedRequest) } returns mockk()

        replyService.findReplyAggregatesByComment(comment, paginationRequest)

        verify { replyAggregateView.findAll(expectedRequest) }
    }

    @Test
    fun `should handle add like operations correctly`() {
        val reply = mockk<Reply> { every { id } returns "test-reply-id" }
        val user = mockk<User> { every { id } returns "test-user-id" }

        // Test successful like operation
        val like = mockk<Like>()
        every { likeRepository.addLike("test-reply-id", "test-user-id") } returns like
        every { replyAggregateView.refreshView("test-reply-id") } returns mockk()

        replyService.addLike(reply, user)
        verify {
            likeRepository.addLike("test-reply-id", "test-user-id")
            replyAggregateView.refreshView("test-reply-id")
        }

        // Test unsuccessful like operation (already liked)
        every { likeRepository.addLike("test-reply-id", "test-user-id") } returns null

        replyService.addLike(reply, user)
        verify(exactly = 1) { replyAggregateView.refreshView("test-reply-id") } // Should not be called again
    }

    @Test
    fun `should handle remove like operations correctly`() {
        val reply = mockk<Reply> { every { id } returns "test-reply-id" }
        val user = mockk<User> { every { id } returns "test-user-id" }

        // Test successful unlike operation
        every { likeRepository.removeLike("test-reply-id", "test-user-id") } returns true
        every { replyAggregateView.refreshView("test-reply-id") } returns mockk()

        replyService.removeLike(reply, user)
        verify {
            likeRepository.removeLike("test-reply-id", "test-user-id")
            replyAggregateView.refreshView("test-reply-id")
        }

        // Test unsuccessful unlike operation (not liked)
        every { likeRepository.removeLike("test-reply-id", "test-user-id") } returns false

        replyService.removeLike(reply, user)
        verify(exactly = 1) { replyAggregateView.refreshView("test-reply-id") } // Should not be called again
    }
}

