package com.kogo.content.storage.view

import com.kogo.content.storage.entity.Post
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "post_stats")
data class PostAggregate(
    @Id
    val postId: String,
    val post: Post,
    val likedUserIds: Set<String> = emptySet(),
    val viewerIds: Set<String> = emptySet(),
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    val commentIds: Set<String> = emptySet(),
    val commentCount: Int = 0,
    val replyIds: Set<String> = emptySet(),
    val replyCount: Int = 0,
    val popularityScore: Double = 0.0,
    val lastUpdated: Instant = Instant.now()
)
