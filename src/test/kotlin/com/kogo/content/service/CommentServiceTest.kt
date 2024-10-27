package com.kogo.content.service

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.service.entity.CommentService
import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.entity.CommentParentType
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.LikeRepository
import com.kogo.content.storage.repository.PostRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

class CommentServiceTest {
    private val commentRepository: CommentRepository = mockk()
    private val postRepository: PostRepository = mockk()
    private val likeRepository: LikeRepository = mockk()
    private val commentService: CommentService = CommentService(commentRepository, postRepository, likeRepository)

    @Test
    fun `should find comments by parentId`() {
        val parentId = "parent-id"
        val user = mockk<UserDetails>()
        val result = Comment(
            id = "comment-id",
            parentId = parentId,
            content = "comment-content",
            parentType = CommentParentType.POST,
            owner = user,
            replies = emptyList()
        )

        every { commentRepository.findAllByPostId(parentId) } returns listOf(result)

        commentService.listCommentsByPostId(parentId)

        verify {
            commentRepository.findAllByPostId(withArg {
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
        val post = mockk<Post>(relaxed = true) {
            every { comments } returns emptyList() // Mock comments list
        }

        every { postRepository.findByIdOrNull(postId) } returns post
        every { commentRepository.save(any()) } returns mockk()
        every { postRepository.save(post) } returns post

        // Act
        commentService.create(postId, CommentParentType.POST, user, commentDto)

        verify {
            commentRepository.save(withArg {
                assertThat(it.content).isEqualTo("comment content")
                assertThat(it.owner).isEqualTo(user)
                assertThat(it.parentId).isEqualTo("post-id")
                assertThat(it.parentType).isEqualTo(CommentParentType.POST)
                assertThat(it.replies).isEmpty()
            })
        }
        verify {
            postRepository.save(post)
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
            owner = user,
            replies = emptyList()
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
    fun `should delete a comment`() {
        val user = mockk<UserDetails>()
        val comment = Comment(
            id = "comment-id",
            parentId = "parent-id",
            parentType = CommentParentType.POST,
            content = "comment-content",
            owner = user,
            replies = listOf("reply-id-1", "reply-id-2")
        )
        val post = mockk<Post>(relaxed = true) {
            every { comments } returns listOf(comment)
        }
        val replies = listOf(
            Comment(id = "reply-id-1", parentId = comment.id!!, parentType = CommentParentType.COMMENT, replies = emptyList(), content = "comment-content", owner = user,),
            Comment(id = "reply-id-2", parentId = comment.id!!, parentType = CommentParentType.COMMENT, replies = emptyList(), content = "comment-content", owner = user,)
        )

        // Mock repository calls
        every { postRepository.findByIdOrNull(comment.parentId) } returns post
        every { commentRepository.findByIdOrNull(comment.id!!) } returns comment
        every { commentRepository.findAllById(comment.replies) } returns replies
        every { commentRepository.deleteById(any()) } just Runs

        every { postRepository.save(post) } returns post
        every { commentRepository.save(any()) } returns comment

        // Act
        commentService.delete(comment)

        verify {
            commentRepository.deleteById(comment.id!!)
            replies.forEach { reply ->
                commentRepository.deleteById(reply.id!!)
            }
            postRepository.save(post)
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
