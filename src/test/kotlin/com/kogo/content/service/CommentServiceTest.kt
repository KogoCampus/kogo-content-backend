package com.kogo.content.service

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.entity.CommentParentType
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.LikeRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommentServiceTest {
    private val commentRepository: CommentRepository = mockk()
    private val likeRepository: LikeRepository = mockk()
    private val commentService: CommentService = CommentService(commentRepository, likeRepository,)

    @Test
    fun `should find comments by parentId`() {
        val parentId = "parent-id"
        val user = mockk<UserDetails>()
        val result = Comment(
            id = "comment-id",
            parentId = parentId,
            content = "comment-content",
            parentType = CommentParentType.POST,
            author = user,
        )

        every { commentRepository.findAllByParentId(parentId) } returns listOf(result)

        commentService.findCommentsByParentId(parentId)

        verify {
            commentRepository.findAllByParentId(withArg {
                assertThat(it).isEqualTo(parentId)
            })
        }
    }

    @Test
    fun `should create a comment`() {
        val postId = "post-id"
        val user = mockk<UserDetails>()
        val commentDto = mockk<CommentDto>() {
            every { content } returns "comment content"
        }
        every { commentRepository.save(any()) } returns mockk()
        commentService.create(postId, CommentParentType.POST, user, commentDto)

        verify {
            commentRepository.save(withArg {
                assertThat(it.content).isEqualTo("comment content")
                assertThat(it.author).isEqualTo(user)
                assertThat(it.parentId).isEqualTo("post-id")
                assertThat(it.parentType).isEqualTo(CommentParentType.POST)
            })
        }
    }

    @Test
    fun `should update a comment`() {
        val user = mockk<UserDetails>()
        val comment = Comment(
            id = "comment-id",
            parentId = "parent-id",
            parentType = CommentParentType.POST,
            content = "comment-content",
            author = user,
        )

        val newComment = CommentUpdate(
            content = "new-comment-content",
        )

        every { commentRepository.save(any()) } returns comment

        commentService.update(comment, newComment)
        verify {
            commentRepository.save(withArg {
                assertThat(it.content).isEqualTo(newComment.content)
            })
        }
    }

    @Test
    fun `should add a like to a comment`() {
        val commentId = "comment-id"
        val userId = "user-id"
        val user = mockk<UserDetails>() {
            every { id } returns userId
        }

        every { likeRepository.save(any()) } returns mockk()
        every { commentRepository.addLike(any()) } just Runs

        commentService.addLike(commentId, user)

        verify {
            likeRepository.save(withArg {
                assertThat(it.parentId).isEqualTo(commentId)
                assertThat(it.userId).isEqualTo(userId)
            })
            commentRepository.addLike(commentId)
        }
    }
}
