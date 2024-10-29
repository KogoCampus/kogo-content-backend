package com.kogo.content.service.entity

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.service.CommentService
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PageToken
import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.PostRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull

class CommentServiceTest {
    private val commentRepository: CommentRepository = mockk()
    private val postRepository: PostRepository = mockk()

    private val commentService = CommentService(commentRepository, postRepository)

    @BeforeEach
    fun setup() {
        clearMocks(commentRepository)
        clearMocks(postRepository)
    }

    @Test
    fun `should find comment by id`() {
        val commentId = "test-comment-id"
        val expectedComment = mockk<Comment>()

        every { commentRepository.findByIdOrNull(commentId) } returns expectedComment

        val result = commentService.find(commentId)

        assertThat(result).isEqualTo(expectedComment)
        verify { commentRepository.findByIdOrNull(commentId) }
    }

    @Test
    fun `should return paginated list of comments by post`() {
        val post = mockk<Post> { every { id } returns "test-post-id" }
        val comment1 = mockk<Comment> { every { id } returns "comment-1" }
        val comment2 = mockk<Comment> { every { id } returns "comment-2" }
        val comments = listOf(comment1, comment2)

        val paginationRequest = PaginationRequest(limit = 2, pageToken = PageToken())
        val pageable = PageRequest.of(0, paginationRequest.limit, Sort.by(Sort.Direction.DESC, "_id"))

        every { commentRepository.findAllByPostId("test-post-id", pageable) } returns comments

        val result = commentService.listCommentsByPost(post, paginationRequest)

        assertThat(result.items).hasSize(2)
        assertThat(result.nextPage?.pageLastResourceId).isEqualTo("comment-2")
        verify { commentRepository.findAllByPostId("test-post-id", pageable) }
    }

    @Test
    fun `should create a new comment`() {
        val post = Post(
            id = "test-post-id",
            title = "Test Post",
            content = "Test Content",
            author = mockk(),
            topic = mockk(),
            commentCount = 0
        )
        val author = mockk<UserDetails>()
        val commentDto = CommentDto(content = "Test Comment")

        every { postRepository.save(any()) } returns post
        every { commentRepository.save(any()) } answers { firstArg() }

        val result = commentService.create(post, author, commentDto)

        assertThat(result.content).isEqualTo(commentDto.content)
        assertThat(result.author).isEqualTo(author)
        assertThat(post.commentCount).isEqualTo(1)
        verify {
            postRepository.save(post)
            commentRepository.save(any())
        }
    }

    @Test
    fun `should update an existing comment`() {
        val comment = Comment(
            id = "test-comment-id",
            content = "Old Content",
            post = mockk(),
            author = mockk()
        )
        val commentUpdate = CommentUpdate(content = "Updated Content")

        every { commentRepository.save(any()) } returns comment

        val result = commentService.update(comment, commentUpdate)

        assertThat(result.content).isEqualTo("Updated Content")
        verify { commentRepository.save(comment) }
    }

    @Test
    fun `should delete a comment`() {
        val post = Post(
            id = "test-post-id",
            commentCount = 1,
            title = "Test Post",
            content = "Test Content",
            author = mockk(),
            topic = mockk()
        )
        val comment = Comment(
            id = "test-comment-id",
            content = "Test Content",
            post = post,
            author = mockk()
        )

        every { postRepository.save(any()) } returns post
        every { commentRepository.deleteById(comment.id!!) } just Runs

        commentService.delete(comment)

        assertThat(post.commentCount).isEqualTo(0)
        verify {
            postRepository.save(post)
            commentRepository.deleteById(comment.id!!)
        }
    }

    @Test
    fun `should add like to comment`() {
        val user = mockk<UserDetails> { every { id } returns "test-user-id" }
        val comment = Comment(
            id = "test-comment-id",
            content = "Test Content",
            post = mockk(),
            author = mockk(),
            likes = 0
        )

        every { commentRepository.addLike("test-comment-id", "test-user-id") } returns mockk()
        every { commentRepository.save(any()) } returns comment

        commentService.addLike(comment, user)

        assertThat(comment.likes).isEqualTo(1)
        verify {
            commentRepository.addLike("test-comment-id", "test-user-id")
            commentRepository.save(comment)
        }
    }

    @Test
    fun `should remove like from comment`() {
        val user = mockk<UserDetails> { every { id } returns "test-user-id" }
        val comment = Comment(
            id = "test-comment-id",
            content = "Test Content",
            post = mockk(),
            author = mockk(),
            likes = 1
        )

        every { commentRepository.removeLike("test-comment-id", "test-user-id") } returns true
        every { commentRepository.save(any()) } returns comment

        commentService.removeLike(comment, user)

        assertThat(comment.likes).isEqualTo(0)
        verify {
            commentRepository.removeLike("test-comment-id", "test-user-id")
            commentRepository.save(comment)
        }
    }
}

