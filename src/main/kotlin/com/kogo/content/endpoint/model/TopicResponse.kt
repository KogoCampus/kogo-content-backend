package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.Topic
import java.time.Instant

data class TopicResponse(
    var id: String,
    var topicName: String,
    var description: String,
    var tags: List<String> = emptyList(),
    var profileImage: AttachmentResponse? = null,
    var createdAt: Instant,
    var updatedAt: Instant,
    var followerCount: Int,
    var owner: UserData.Public,
    var userActivity: TopicUserActivity?,
) {
    companion object {
        fun from(topic: Topic, userActivity: TopicUserActivity? = null): TopicResponse = TopicResponse(
            id = topic.id!!,
            owner = UserData.Public.from(topic.owner),
            topicName = topic.topicName,
            description = topic.description,
            tags = topic.tags,
            profileImage = topic.profileImage?.let { AttachmentResponse.from(it) },
            createdAt = topic.createdAt,
            updatedAt = topic.updatedAt,
            followerCount = topic.followerCount,
            userActivity = userActivity
        )
    }

    data class TopicUserActivity(
        val followed: Boolean,
        val followedAt: Instant?,
    )
}
