package com.kogo.content.storage.view

import com.kogo.content.storage.entity.Reply
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "reply_stats")
data class ReplyAggregate(
    @Id
    val replyId: String,
    val reply: Reply,
    val likedUserIds: Set<String> = emptySet(),
    val likeCount: Int = 0,
    val lastUpdated: Instant = Instant.now()
)
