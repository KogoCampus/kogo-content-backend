package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.User
import com.kogo.content.storage.view.PostAggregate
import java.time.Instant

data class PostResponse(
    var id: String,
    var author: UserData.Public,
    var topicId: String? = null,
    var topicName: String? = null,
    var title: String,
    var content: String,
    var attachments: List<AttachmentResponse>,
    var comments: Int,
    var createdAt: Instant,
    var updatedAt: Instant,
    val viewCount: Int,
    val likeCount: Int,
    val popularityScore: Double? = null,
    val likedByCurrentUser: Boolean = false,
    val viewedByCurrentUser: Boolean = false
) {
    companion object {
        fun create(postView: PostAggregate, currentUser: User): PostResponse = PostResponse(
            id = postView.postId,
            topicId = postView.post.topic.id,
            author = UserData.Public.from(postView.post.author),
            title = postView.post.title,
            topicName = postView.post.topic.topicName,
            content = postView.post.content,
            attachments = postView.post.attachments.map { AttachmentResponse.create(it) },
            viewCount = postView.viewCount,
            likeCount = postView.likeCount,
            comments = postView.commentCount + postView.replyCount,
            popularityScore = postView.popularityScore,
            likedByCurrentUser = postView.likedUserIds.contains(currentUser.id),
            viewedByCurrentUser = postView.viewerIds.contains(currentUser.id),
            createdAt = postView.post.createdAt,
            updatedAt = postView.post.updatedAt
        )
    }
}
