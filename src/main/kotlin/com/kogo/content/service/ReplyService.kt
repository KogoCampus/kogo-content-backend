package com.kogo.content.service

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
class ReplyService @Autowired constructor(
    private val replyRepository: ReplyRepository,
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
) {
    fun find(replyId: String): Reply? {
        return replyRepository.findByIdOrNull(replyId)
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
    fun create(comment: Comment, author: UserDetails, reply: CommentDto) = run {
        comment.replyCount += 1
        commentRepository.save(comment)
        replyRepository.save(Reply(
            comment = comment,
            author = author,
            content = reply.content,
        ))
    }

    @Transactional
    fun update(reply: Reply, commentUpdate: CommentUpdate) = run {
        commentUpdate.let { reply.content = it.content }
        reply.updatedAt = Instant.now()
        replyRepository.save(reply)
    }

    fun delete(reply: Reply) = run {
        val comment = reply.comment
        comment.replyCount -= 1
        commentRepository.save(comment)
        replyRepository.deleteById(reply.id!!)
    }

    fun addLike(reply: Reply, user: UserDetails) = run {
        replyRepository.addLike(reply.id!!, user.id!!)?.let {
            reply.likes += 1
            replyRepository.save(reply)
            it
        }
    }

    fun removeLike(reply: Reply, user: UserDetails) = run {
        if (replyRepository.removeLike(reply.id!!, user.id!!)) {
            reply.likes -= 1
            replyRepository.save(reply)
        }
    }

    fun isUserAuthor(reply: Reply, user: UserDetails): Boolean = reply.author == user
    fun hasUserLikedReply(reply: Reply, user: UserDetails) = commentRepository.findLike(reply.id!!, user.id!!) != null
}
