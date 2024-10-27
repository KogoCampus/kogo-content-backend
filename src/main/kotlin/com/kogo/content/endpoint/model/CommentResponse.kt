package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.Comment
import java.time.Instant

data class CommentResponse (
    var id: String,
    var author: UserInfo,
    var postId: String,
    var content: String,
    val likes: Int,
    var createdAt: Instant,
    var updatedAt: Instant,
    var repliesCount: Int
) {
    companion object {
        fun from(comment: Comment): CommentResponse = CommentResponse(
            id = comment.id!!,
            author = UserInfo.from(comment.author),
            content = comment.content,
            postId = comment.post.id!!,
            createdAt = comment.createdAt!!,
            updatedAt = comment.updatedAt!!,
            likes = comment.likes,
            repliesCount = comment.replyCount
        )
    }
}
