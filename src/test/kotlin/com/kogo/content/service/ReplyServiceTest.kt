package com.kogo.content.service

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PageToken
import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.entity.Reply
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.PostRepository
import com.kogo.content.storage.repository.ReplyRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull

class ReplyServiceTest {
    private val replyRepository: ReplyRepository = mockk()
    private val commentRepository: CommentRepository = mockk()
    private val postRepository: PostRepository = mockk()

    private val replyService = ReplyService(replyRepository, commentRepository, postRepository)

    @BeforeEach
    fun setup() {
        clearMocks(replyRepository)
        clearMocks(commentRepository)
        clearMocks(postRepository)
    }

    @Test
    fun `should find reply by id`() {
        val replyId = "test-reply-id"
        val expectedReply = mockk<Reply>()

        every { replyRepository.findByIdOrNull(replyId) } returns expectedReply

        val result = replyService.find(replyId)

        assertThat(result).isEqualTo(expectedReply)
        verify { replyRepository.findByIdOrNull(replyId) }
    }

    @Test
    fun `should return paginated list of replies by comment`() {
        val comment = mockk<Comment> { every { id } returns "test-comment-id" }
        val reply1 = mockk<Reply> { every { id } returns "reply-1" }
        val reply2 = mockk<Reply> { every { id } returns "reply-2" }
        val replies = listOf(reply1, reply2)

        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken())
        val pageable = PageRequest.of(0, paginationRequest.limit, Sort.by(Sort.Direction.DESC, "_id"))

        every { replyRepository.findAllByCommentId("test-comment-id", pageable) } returns replies

        val result = replyService.listRepliesByComment(comment, paginationRequest)

        assertThat(result.items).hasSize(2)
        assertThat(result.nextPage?.pageLastResourceId).isEqualTo("reply-2")
        verify { replyRepository.findAllByCommentId("test-comment-id", pageable) }
    }

    @Test
    fun `should create a new reply`() {
        val comment = Comment(
            id = "test-comment-id",
            content = "Test Comment",
            author = mockk(),
            post = mockk(),
            replyCount = 0
        )
        val author = mockk<UserDetails>()
        val replyDto = CommentDto(content = "Test Reply")

        every { commentRepository.save(any()) } returns comment
        every { replyRepository.save(any()) } answers { firstArg() }

        val result = replyService.create(comment, author, replyDto)

        assertThat(result.content).isEqualTo(replyDto.content)
        assertThat(result.author).isEqualTo(author)
        assertThat(comment.replyCount).isEqualTo(1)
        verify {
            commentRepository.save(comment)
            replyRepository.save(any())
        }
    }

    @Test
    fun `should update an existing reply`() {
        val reply = Reply(
            id = "test-reply-id",
            content = "Old Content",
            comment = mockk(),
            author = mockk()
        )
        val replyUpdate = CommentUpdate(content = "Updated Content")

        every { replyRepository.save(any()) } returns reply

        val result = replyService.update(reply, replyUpdate)

        assertThat(result.content).isEqualTo("Updated Content")
        verify { replyRepository.save(reply) }
    }

    @Test
    fun `should delete a reply`() {
        val comment = Comment(
            id = "test-comment-id",
            content = "Test Comment",
            author = mockk(),
            post = mockk(),
            replyCount = 1
        )
        val reply = Reply(
            id = "test-reply-id",
            content = "Test Content",
            comment = comment,
            author = mockk()
        )

        every { commentRepository.save(any()) } returns comment
        every { replyRepository.deleteById("test-reply-id") } just Runs

        replyService.delete(reply)

        assertThat(comment.replyCount).isEqualTo(0)
        verify {
            commentRepository.save(comment)
            replyRepository.deleteById("test-reply-id")
        }
    }

    @Test
    fun `should add like to reply`() {
        val user = mockk<UserDetails>() { every { id } returns "test-user-id" }
        val reply = Reply(
            id = "test-reply-id",
            content = "Test Content",
            comment = mockk(),
            author = mockk(),
            likes = 0
        )

        every { replyRepository.addLike("test-reply-id", "test-user-id") } returns mockk()
        every { replyRepository.save(any()) } returns reply

        replyService.addLike(reply, user)

        assertThat(reply.likes).isEqualTo(1)
        verify {
            replyRepository.addLike("test-reply-id", "test-user-id")
            replyRepository.save(reply)
        }
    }

    @Test
    fun `should remove like from reply`() {
        val user = mockk<UserDetails>() { every { id } returns "test-user-id" }
        val reply = Reply(
            id = "test-reply-id",
            content = "Test Content",
            comment = mockk(),
            author = mockk(),
            likes = 1
        )

        every { replyRepository.removeLike("test-reply-id", "test-user-id") } returns true
        every { replyRepository.save(any()) } returns reply

        replyService.removeLike(reply, user)

        assertThat(reply.likes).isEqualTo(0)
        verify {
            replyRepository.removeLike("test-reply-id", "test-user-id")
            replyRepository.save(reply)
        }
    }
}

