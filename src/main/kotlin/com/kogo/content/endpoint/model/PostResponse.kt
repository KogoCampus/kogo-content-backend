package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.Post
import java.time.Instant

data class PostResponse(
    var id: String,
    var author: UserData.Public,
    var topicId: String? = null,
    var topicName: String?= null,
    var title: String,
    var content: String,
    var attachments: List<AttachmentResponse>,
    var commentCount: Int,
    var createdAt: Instant,
    var updatedAt: Instant,
    val viewCount: Int,
    val likes: Int,
    val userActivity: PostUserActivity?
) {
    companion object {
        fun from(post: Post, userActivity: PostUserActivity? = null): PostResponse = PostResponse(
            id = post.id!!,
            topicId = post.topic.id,
            author = UserData.Public.from(post.author),
            title = post.title,
            topicName = post.topic.topicName,
            content = post.content,
            attachments = post.attachments.map { AttachmentResponse.from(it) },
            viewCount = post.viewCount,
            commentCount = post.commentCount,
            likes = post.likes,
            createdAt = post.createdAt!!,
            updatedAt = post.updatedAt!!,
            userActivity = userActivity
        )
    }

    data class PostUserActivity(
        val liked: Boolean,
        val likedAt: Instant?,
        val viewed: Boolean,
        val viewedAt: Instant?
    )
}
