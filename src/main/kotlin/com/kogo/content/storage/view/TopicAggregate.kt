package com.kogo.content.storage.view

import com.kogo.content.storage.entity.Topic
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "topic_stats")
data class TopicAggregate(
    @Id
    val topicId: String,
    val topic: Topic,
    val followerIds: Set<String> = emptySet(),
    val followerCount: Int = 0,
    val postIds: Set<String> = emptySet(),
    val postCount: Int = 0,
    val lastUpdated: Instant = Instant.now()
)
