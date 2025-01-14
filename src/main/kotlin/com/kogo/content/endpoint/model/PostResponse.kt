package com.kogo.content.endpoint.model

import com.kogo.content.storage.model.Comment
import com.kogo.content.storage.model.entity.Post
import com.kogo.content.storage.model.Reply
import com.kogo.content.storage.model.entity.User

data class PostResponse(
    var id: String,
    var author: UserData.Public,
    var group: GroupResponse,
    var title: String,
    var content: String,
    var attachments: List<AttachmentResponse>,
    var comments: List<CommentResponse>,
    var likeCount: Int,
    var likedByCurrentUser: Boolean = false,
    var viewCount: Int,
    var viewedByCurrentUser: Boolean = false,
    var createdAt: Long,
    var updatedAt: Long,
    var isAuthorBlacklistedByCurrentUser: Boolean = false,
) {
    companion object {
        fun from(post: Post, currentUser: User): PostResponse {
            val response = PostResponse(
                id = post.id!!,
                author = UserData.Public.from(post.author),
                group = GroupResponse.from(post.group, currentUser),
                title = post.title,
                content = post.content,
                attachments = post.attachments.map { AttachmentResponse.from(it) },
                comments = post.comments.map { CommentResponse.from(it, currentUser) },
                likeCount = post.activeLikes.size,
                likedByCurrentUser = post.activeLikes.any { it.userId == currentUser.id },
                viewCount = post.viewerIds.size,
                viewedByCurrentUser = post.viewerIds.contains(currentUser.id),
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
            if (currentUser.blacklistedUserIds.contains(post.author.id)) {
                response.title = ""
                response.content = ""
                response.isAuthorBlacklistedByCurrentUser = true
            }
            return response
        }
    }
}

data class CommentResponse (
    var id: String,
    var author: UserData.Public,
    var content: String,
    var replies: List<ReplyResponse>,
    var likeCount: Int,
    var likedByCurrentUser: Boolean = false,
    var createdAt: Long,
    var updatedAt: Long,
    var isAuthorBlacklistedByCurrentUser: Boolean = false,
) {
    companion object {
        fun from(comment: Comment, currentUser: User): CommentResponse {
            val response = CommentResponse(
                id = comment.id,
                author = UserData.Public.from(comment.author),
                content = comment.content,
                replies = comment.replies.map { ReplyResponse.from(it, currentUser) },
                likeCount = comment.activeLikes.size,
                likedByCurrentUser = comment.activeLikes.any { it.userId == currentUser.id },
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt
            )
            if (currentUser.blacklistedUserIds.contains(comment.author.id)) {
                response.content = ""
                response.isAuthorBlacklistedByCurrentUser = true
            }
            return response
        }
    }
}

data class ReplyResponse (
    var id: String,
    var author: UserData.Public,
    var content: String,
    var likeCount: Int,
    var likedByCurrentUser: Boolean = false,
    var createdAt: Long,
    var updatedAt: Long,
    var isAuthorBlacklistedByCurrentUser: Boolean = false,
) {
    companion object {
        fun from(reply: Reply, currentUser: User): ReplyResponse {
            val response = ReplyResponse(
                id = reply.id,
                author = UserData.Public.from(reply.author),
                content = reply.content,
                likeCount = reply.activeLikes.size,
                likedByCurrentUser = reply.activeLikes.any { it.userId == currentUser.id },
                createdAt = reply.createdAt,
                updatedAt = reply.updatedAt
            )
            if (currentUser.blacklistedUserIds.contains(reply.author.id)) {
                response.content = ""
                response.isAuthorBlacklistedByCurrentUser = true
            }
            return response
        }
    }
}

