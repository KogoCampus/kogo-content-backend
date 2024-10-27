package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.Reply
import java.time.Instant

data class ReplyResponse (
    var id: String,
    var author: UserInfo,
    var commentId: String,
    var content: String,
    val likes: Int,
    var createdAt: Instant,
    var updatedAt: Instant,
) {
    companion object {
        fun from(reply: Reply): ReplyResponse = ReplyResponse(
            id = reply.id!!,
            author = UserInfo.from(reply.author),
            content = reply.content,
            commentId = reply.comment.id!!,
            createdAt = reply.createdAt!!,
            updatedAt = reply.updatedAt!!,
            likes = reply.likes,
        )
    }
}
