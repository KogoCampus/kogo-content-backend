package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.Topic
import java.time.Instant

data class TopicResponse(
    var id: String,
    var owner: UserInfo,
    var topicName: String,
    var description: String,
    var tags: List<String> = emptyList(),
    var profileImage: AttachmentResponse? = null,
    var createdAt: Instant,
    var updatedAt: Instant,
    var followingUserCount: Int,
) {
    companion object {
        fun from(topic: Topic): TopicResponse = TopicResponse(
            id = topic.id!!,
            owner = UserInfo.from(topic.owner),
            topicName = topic.topicName,
            description = topic.description,
            tags = topic.tags,
            profileImage = topic.profileImage?.let { AttachmentResponse.from(it) },
            createdAt = topic.createdAt!!,
            updatedAt = topic.updatedAt!!,
            followingUserCount = topic.followingUserCount
        )
    }
}
