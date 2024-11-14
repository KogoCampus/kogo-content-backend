package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.Reply
import com.kogo.content.storage.entity.User
import com.kogo.content.storage.view.ReplyAggregate
import java.time.Instant

data class ReplyResponse (
    var id: String,
    var author: UserData.Public,
    var commentId: String,
    var content: String,
    val likeCount: Int,
    val likedByCurrentUser: Boolean = false,
    var createdAt: Instant,
    var updatedAt: Instant,
) {
    companion object {
        fun create(replyView: ReplyAggregate, currentUser: User) = ReplyResponse(
            id = replyView.replyId,
            author = UserData.Public.from(replyView.reply.author),
            commentId = replyView.reply.comment.id!!,
            content = replyView.reply.content,
            likeCount = replyView.likeCount,
            likedByCurrentUser = replyView.likedUserIds.contains(replyView.reply.author.id),
            createdAt = replyView.reply.createdAt,
            updatedAt = replyView.reply.updatedAt,
        )
    }
}
