package com.kogo.content.endpoint.model

import java.time.Instant
import java.time.LocalDateTime

data class PostResponse(
    var id: String,
    var authorUserId: String? = null,
    var topicId: String? = null,
    var title: String,
    var content: String,
    var attachments: List<PostAttachment>,
    var comments: List<PostComment>,
    var createdAt: Instant,
    val viewcount: Int,
    val likes: Int,
) {
    data class PostAttachment (
        val attachmentId: String? = null,
        val name: String,
        val size: Long,
        val contentType: String,
        val url: String
    )
    data class PostComment (
        var commentId: String? = null,
        var authorId: String? = null,
        var replyCount: Int,
    )
}
