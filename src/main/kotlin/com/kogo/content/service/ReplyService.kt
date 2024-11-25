package com.kogo.content.service

import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.ReplyRepository
import com.kogo.content.storage.repository.LikeRepository
import com.kogo.content.storage.repository.ViewerRepository
import com.kogo.content.storage.view.CommentAggregateView
import com.kogo.content.storage.view.PostAggregateView
import com.kogo.content.storage.view.ReplyAggregate
import com.kogo.content.storage.view.ReplyAggregateView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ReplyService @Autowired constructor(
    private val replyRepository: ReplyRepository,
    private val likeRepository: LikeRepository,
    private val replyAggregateView: ReplyAggregateView,
    private val viewerRepository: ViewerRepository,
    private val commentAggregateView: CommentAggregateView,
    private val postAggregateView: PostAggregateView
) {
    fun find(replyId: String) = replyRepository.findByIdOrNull(replyId)
    fun findAggregate(replyId: String) = replyAggregateView.find(replyId)

    fun findReplyAggregatesByComment(comment: Comment, paginationRequest: PaginationRequest): PaginationSlice<ReplyAggregate>
        = replyAggregateView.findAll(
            paginationRequest.withFilter("comment", comment.id!!)
        )

    @Transactional
    fun create(comment: Comment, author: User, dto: CommentDto): Reply {
        val reply = replyRepository.save(
            Reply(
                comment = comment,
                author = author,
                content = dto.content,
            )
        )
        replyAggregateView.refreshView(reply.id!!)
        commentAggregateView.refreshView(comment.id!!)
        postAggregateView.refreshView(comment.post.id!!)
        return reply
    }

    @Transactional
    fun update(reply: Reply, commentUpdate: CommentUpdate): Reply {
        commentUpdate.let { reply.content = it.content }
        reply.updatedAt = Instant.now()
        val updatedReply = replyRepository.save(reply)
        replyAggregateView.refreshView(reply.id!!)
        return updatedReply
    }

    fun delete(reply: Reply) {
        replyRepository.deleteById(reply.id!!)
        replyAggregateView.delete(reply.id!!)
        commentAggregateView.refreshView(reply.comment.id!!)
        postAggregateView.refreshView(reply.comment.post.id!!)
    }

    fun addLike(reply: Reply, user: User): Like? {
        val like = likeRepository.addLike(reply.id!!, user.id!!)
        if (like != null) {
            replyAggregateView.refreshView(reply.id!!)
        }
        return like
    }

    fun removeLike(reply: Reply, user: User) {
        val removed = likeRepository.removeLike(reply.id!!, user.id!!)
        if (removed) {
            replyAggregateView.refreshView(reply.id!!)
        }
    }

    fun markReplyViewedByUser(replyId: String, userId: String): Viewer? {
        val viewed = viewerRepository.addView(replyId, userId)
        if (viewed != null)
            replyAggregateView.refreshView(replyId)
        return viewed
    }

    fun hasUserLikedReply(reply: Reply, user: User) = likeRepository.findLike(reply.id!!, user.id!!) != null
    fun isUserAuthor(reply: Reply, user: User): Boolean = reply.author == user
}
