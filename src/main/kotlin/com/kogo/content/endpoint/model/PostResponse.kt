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
    val viewcount: Int,
    val likes: Int,
) {
    companion object {
        fun from(post: Post): PostResponse = PostResponse(
            id = post.id!!,
            topicId = post.topic.id,
            author = UserData.Public.from(post.author),
            title = post.title,
            topicName = post.topic.topicName,
            content = post.content,
            attachments = post.attachments.map { AttachmentResponse.from(it) },
            viewcount = post.viewCount,
            commentCount = post.commentCount,
            likes = post.likes,
            createdAt = post.createdAt!!,
            updatedAt = post.updatedAt!!
        )
    }
}
