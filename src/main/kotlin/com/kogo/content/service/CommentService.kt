package com.kogo.content.service

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.LikeRepository
import org.jetbrains.kotlin.util.profile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CommentService @Autowired constructor(
    private val commentRepository: CommentRepository,
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
            createdAt = Instant.now()
        )
        return commentRepository.save(newComment)
    }

    @Transactional
    fun delete(commentId: String) {
        commentRepository.deleteById(commentId)
    }

    @Transactional
    fun update(comment: Comment, commentUpdate: CommentUpdate): Comment {
        commentUpdate.let { comment.content = it.content }
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
}
