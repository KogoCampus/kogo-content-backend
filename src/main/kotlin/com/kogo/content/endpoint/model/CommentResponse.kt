package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.User
import com.kogo.content.storage.view.CommentAggregate
import java.time.Instant

data class CommentResponse(
    var id: String,
    var author: UserData.Public,
    var postId: String,
    var content: String,
    val likeCount: Int,
    var replyCount: Int,
    var createdAt: Instant,
    var updatedAt: Instant,
    val likedByCurrentUser: Boolean = false
) {
    companion object {
        fun create(commentView: CommentAggregate, currentUser: User) = CommentResponse(
            id = commentView.commentId,
            author = UserData.Public.from(commentView.comment.author),
            postId = commentView.comment.post.id!!,
            content = commentView.comment.content,
            likeCount = commentView.likeCount,
            replyCount = commentView.replyCount,
            createdAt = commentView.comment.createdAt,
            updatedAt = commentView.comment.updatedAt,
            likedByCurrentUser = commentView.likedUserIds.contains(currentUser.id)
        )
    }
}
