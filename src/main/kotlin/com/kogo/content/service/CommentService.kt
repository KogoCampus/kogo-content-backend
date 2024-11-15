package com.kogo.content.service

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.SortDirection
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.LikeRepository
import com.kogo.content.storage.view.CommentAggregate
import com.kogo.content.storage.view.CommentAggregateView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CommentService @Autowired constructor(
    private val commentRepository: CommentRepository,
    private val likeRepository: LikeRepository,
    private val commentAggregateView: CommentAggregateView
) {
    fun find(commentId: String): Comment? = commentRepository.findByIdOrNull(commentId)
    fun findAggregate(commentId: String): CommentAggregate = commentAggregateView.find(commentId)

    fun findAggregatesByPost(post: Post, paginationRequest: PaginationRequest) = commentAggregateView.findAll(
        paginationRequest.withFilter("post", post.id!!)
            .withSort("createdAt", SortDirection.DESC)
    )

    @Transactional
    fun create(post: Post, author: User, dto: CommentDto): Comment {
        val comment = commentRepository.save(
            Comment(
                post = post,
                author = author,
                content = dto.content,
            )
        )
        commentAggregateView.refreshView(comment.id!!)
        return comment;
    }

    @Transactional
    fun update(comment: Comment, commentUpdate: CommentUpdate): Comment {
        commentUpdate.let { comment.content = it.content }
        comment.updatedAt = Instant.now()
        val updatedComment = commentRepository.save(comment)
        commentAggregateView.refreshView(comment.id!!)
        return updatedComment
    }

    fun delete(comment: Comment) = run {
        commentRepository.deleteById(comment.id!!)
    }

    @Transactional
    fun addLike(comment: Comment, user: User): Like? {
        val like = likeRepository.addLike(comment.id!!, user.id!!)
        if (like != null) {
            commentAggregateView.refreshView(comment.id!!)
        }
        return like
    }

    @Transactional
    fun removeLike(comment: Comment, user: User) {
        val removed = likeRepository.removeLike(comment.id!!, user.id!!)
        if (removed) {
            commentAggregateView.refreshView(comment.id!!)
        }
    }

    fun isUserAuthor(comment: Comment, user: User): Boolean = comment.author == user

    fun hasUserLikedComment(commentId: String, user: User): Boolean {
        return likeRepository.findLike(commentId, user.id!!) != null
    }
}
