package com.kogo.content.service.entity

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationResponse
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.PostRepository
import com.kogo.content.storage.repository.ReplyRepository
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
    private val replyRepository: ReplyRepository,
) {
    fun findComment(commentId: String): Comment? {
        return commentRepository.findByIdOrNull(commentId)
    }

    fun findReply(replyId: String): Reply? {
        return replyRepository.findByIdOrNull(replyId)
    }

    fun listCommentsByPost(post: Post, paginationRequest: PaginationRequest): PaginationResponse<Comment> {
        val limit = paginationRequest.limit
        val pageLastResourceId = paginationRequest.pageToken.pageLastResourceId
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id")) as Pageable
        val comments = if (pageLastResourceId != null) commentRepository.findAllByPostIdAndIdLessThan(post.id!!, pageLastResourceId, pageable)
                        else commentRepository.findAllByPostId(post.id!!, pageable)

        val nextPageToken = comments.lastOrNull()?.let { paginationRequest.pageToken.nextPageToken(it.id!!) }
        return PaginationResponse(comments, nextPageToken)
    }

    fun listRepliesByComment(comment: Comment, paginationRequest: PaginationRequest): PaginationResponse<Reply> {
        val limit = paginationRequest.limit
        val pageLastResourceId = paginationRequest.pageToken.pageLastResourceId
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id")) as Pageable
        val posts = if (pageLastResourceId != null) replyRepository.findAllByCommentIdAndIdLessThan(comment.id!!, pageLastResourceId, pageable)
                    else replyRepository.findAllByCommentId(comment.id!!, pageable)
        val nextPageToken = posts.lastOrNull()?.let { paginationRequest.pageToken.nextPageToken(it.id!!) }
        return PaginationResponse(posts, nextPageToken)
    }

    @Transactional
    fun createComment(post: Post, author: UserDetails, comment: CommentDto) = run {
        post.commentCount += 1
        postRepository.save(post)
        commentRepository.save(Comment(
            post = post,
            author = author,
            content = comment.content,
        ))
    }

    @Transactional
    fun createReply(comment: Comment, author: UserDetails, reply: CommentDto) = run {
        comment.replyCount += 1
        commentRepository.save(comment)
        replyRepository.save(Reply(
            comment = comment,
            author = author,
            content = reply.content,
        ))
    }

    @Transactional
    fun updateComment(comment: Comment, commentUpdate: CommentUpdate) = run {
        commentUpdate.let { comment.content = it.content }
        comment.updatedAt = Instant.now()
        commentRepository.save(comment)
    }

    @Transactional
    fun updateReply(reply: Reply, commentUpdate: CommentUpdate) = run {
        commentUpdate.let { reply.content = it.content }
        reply.updatedAt = Instant.now()
        replyRepository.save(reply)
    }

    fun deleteComment(comment: Comment) = run {
        val post = comment.post
        post.commentCount -= 1
        postRepository.save(post)
        commentRepository.deleteById(comment.id!!)
    }

    fun deleteReply(reply: Reply) = run {
        val comment = reply.comment
        comment.replyCount -= 1
        commentRepository.save(comment)
        replyRepository.deleteById(reply.id!!)
    }

    fun addLikeToComment(comment: Comment, user: UserDetails) = run {
        commentRepository.addLike(comment.id!!, user.id!!)?.let {
            comment.likes += 1
            commentRepository.save(comment)
            it
        }
    }
    fun addLikeToReply(reply: Reply, user: UserDetails) = run {
        replyRepository.addLike(reply.id!!, user.id!!)?.let {
            reply.likes += 1
            replyRepository.save(reply)
            it
        }
    }
    fun removeLikeFromComment(comment: Comment, user: UserDetails) = run {
        if (commentRepository.removeLike(comment.id!!, user.id!!)) {
            comment.likes -= 1
            commentRepository.save(comment)
        }
    }
    fun removeLikeFromReply(reply: Reply, user: UserDetails) = run {
        if (replyRepository.removeLike(reply.id!!, user.id!!)) {
            reply.likes -= 1
            replyRepository.save(reply)
        }
    }

    fun isCommentAuthor(comment: Comment, user: UserDetails): Boolean = comment.author == user
    fun isReplyAuthor(reply: Reply, user: UserDetails): Boolean = reply.author == user

    fun hasUserLikedComment(comment: Comment, user: UserDetails) = commentRepository.findLike(comment.id!!, user.id!!) != null
    fun hasUserLikedReply(reply: Reply, user: UserDetails) = commentRepository.findLike(reply.id!!, user.id!!) != null
}
