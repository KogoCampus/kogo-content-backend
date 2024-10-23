package com.kogo.content.service

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.LikeRepository
import com.kogo.content.storage.repository.PostRepository
import org.jetbrains.kotlin.util.profile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CommentService @Autowired constructor(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val likeRepository: LikeRepository,
) {
    fun findCommentsByParentId(parentId: String): List<Comment> {
        return commentRepository.findAllByParentId(parentId)
    }

    fun find(commentId: String): Comment? {
        return commentRepository.findByIdOrNull(commentId)
    }

    @Transactional
    fun create(parentId: String, parentType: CommentParentType, author: UserDetails, comment: CommentDto): Comment {
        val newComment = Comment(
            content = comment.content,
            parentId = parentId,
            parentType = parentType,
            owner = author,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            replies = emptyList()
        )
        val savedComment = commentRepository.save(newComment)
        if(parentType == CommentParentType.COMMENT) {
            val parentComment = commentRepository.findByIdOrNull(parentId)
            if (parentComment != null) {
                parentComment.replies = parentComment.replies.plus(savedComment.id!!)
                parentComment.repliesCount += 1
                commentRepository.save(parentComment)
            }
        } else {
            val parentPost = postRepository.findByIdOrNull(parentId)
            if(parentPost != null) {
                parentPost.comments = parentPost.comments.plus(savedComment)
                parentPost.commentCount += 1
                postRepository.save(parentPost)
            }
        }
        return savedComment
    }

    @Transactional
    fun delete(comment: Comment) {
        if(comment.parentType == CommentParentType.COMMENT){ // reply
            val parentComment = commentRepository.findByIdOrNull(comment.parentId)
            if (parentComment != null) {
                parentComment.replies = parentComment.replies.minus(comment.id!!)
                parentComment.repliesCount -= 1
                commentRepository.save(parentComment)
            }
        } else { // comment
            val parentPost = postRepository.findByIdOrNull(comment.parentId)
            if(parentPost != null) {
                parentPost.comments = parentPost.comments.minus(comment)
                parentPost.commentCount -= 1
                postRepository.save(parentPost)
            }
            deleteReplies(comment)
        }
        commentRepository.deleteById(comment.id!!)
    }

    @Transactional
    fun update(comment: Comment, commentUpdate: CommentUpdate): Comment {
        commentUpdate.let { comment.content = it.content }
        comment.updatedAt = Instant.now()
        return commentRepository.save(comment)
    }

    @Transactional
    fun addLike(parentId: String, user: UserDetails) {
        val userId = user.id!!
        val like = Like(
            userId = userId,
            parentId = parentId, // commentId
        )
        likeRepository.save(like)
        commentRepository.addLike(parentId)
    }
    @Transactional
    fun removeLike(parentId: String, user: UserDetails) {
        val userId = user.id!!
        val like = likeRepository.findByUserIdAndParentId(userId, parentId)
        likeRepository.deleteById(like!!.id!!)
        commentRepository.removeLike(parentId)
    }

    fun findLikeByUserIdAndParentId(userId: String, parentId: String): Like? =
        likeRepository.findByUserIdAndParentId(userId, parentId)

    @Transactional
    fun deleteReplies(comment: Comment) {
        val replies = commentRepository.findAllById(comment.replies)
        replies.forEach { reply -> delete(reply) }
    }

    fun getReplies(comment: Comment): List<Comment> {
        return commentRepository.findAllById(comment.replies)
    }

    fun isCommentOwner(comment: Comment, user: UserDetails): Boolean = comment.owner == user
}
