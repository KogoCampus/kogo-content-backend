package com.kogo.content.storage.view

import com.kogo.content.storage.entity.Comment
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "comment_stats")
data class CommentAggregate(
    @Id
    val commentId: String,
    val comment: Comment,
    val likedUserIds: Set<String> = emptySet(),
    val replyCount: Int = 0,
    val likeCount: Int = 0,
    val lastUpdated: Instant = Instant.now()
)
