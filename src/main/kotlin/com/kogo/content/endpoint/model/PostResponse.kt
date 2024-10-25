package com.kogo.content.endpoint.model

import java.time.Instant

data class PostResponse(
    var id: String,
    var owner: OwnerInfoResponse,
    var topicId: String? = null,
    var topicName: String?= null,
    var title: String,
    var content: String,
    var attachments: List<AttachmentResponse>,
    var comments: List<PostComment>,
    var commentCount: Int,
    var createdAt: Instant,
    var updatedAt: Instant,
    val viewcount: Int,
    val likes: Int,
) {
    data class PostComment (
        var commentId: String? = null,
        var ownerId: OwnerInfoResponse,
        var replyCount: Int,
    )
}
