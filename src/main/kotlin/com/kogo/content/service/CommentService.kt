package com.kogo.content.service

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationSlice
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.PostRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CommentService @Autowired constructor(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
) {
    fun find(commentId: String): Comment? {
        return commentRepository.findByIdOrNull(commentId)
    }

    fun getAllCommentsByPost(post: Post, paginationRequest: PaginationRequest): PaginationSlice<Comment> {
        val limit = paginationRequest.limit
        val pageLastResourceId = paginationRequest.pageToken.pageLastResourceId
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id")) as Pageable
        val comments = if (pageLastResourceId != null) commentRepository.findAllByPostIdAndIdLessThan(post.id!!, pageLastResourceId, pageable)
                        else commentRepository.findAllByPostId(post.id!!, pageable)

        val nextPageToken = comments.lastOrNull()?.let { paginationRequest.pageToken.nextPageToken(it.id!!) }
        return PaginationSlice(comments, nextPageToken)
    }

    @Transactional
    fun create(post: Post, author: UserDetails, comment: CommentDto) = run {
        post.commentCount += 1
        postRepository.save(post)
        commentRepository.save(Comment(
            post = post,
            author = author,
            content = comment.content,
        ))
    }

    @Transactional
    fun update(comment: Comment, commentUpdate: CommentUpdate) = run {
        commentUpdate.let { comment.content = it.content }
        comment.updatedAt = Instant.now()
        commentRepository.save(comment)
    }

    fun delete(comment: Comment) = run {
        val post = comment.post
        post.commentCount -= 1
        postRepository.save(post)
        commentRepository.deleteById(comment.id!!)
    }

    fun addLike(comment: Comment, user: UserDetails) = run {
        commentRepository.addLike(comment.id!!, user.id!!)?.let {
            comment.likes += 1
            commentRepository.save(comment)
            it
        }
    }

    fun removeLike(comment: Comment, user: UserDetails) = run {
        if (commentRepository.removeLike(comment.id!!, user.id!!)) {
            comment.likes -= 1
            commentRepository.save(comment)
        }
    }

    fun isUserAuthor(comment: Comment, user: UserDetails): Boolean = comment.author == user
    fun hasUserLikedComment(comment: Comment, user: UserDetails) = commentRepository.findLike(comment.id!!, user.id!!) != null
}
