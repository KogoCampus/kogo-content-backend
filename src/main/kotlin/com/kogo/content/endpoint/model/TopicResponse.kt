package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.User
import com.kogo.content.storage.view.TopicAggregate
import java.time.Instant

data class TopicResponse(
    var id: String,
    var topicName: String,
    var description: String,
    var tags: List<String> = emptyList(),
    var profileImage: AttachmentResponse? = null,
    var followerCount: Int,
    var postCount: Int,
    var owner: UserData.Public,
    var followedByCurrentUser: Boolean,
    var createdAt: Instant,
    var updatedAt: Instant,
) {
    companion object {
        fun create(topicAggregate: TopicAggregate, currentUser: User) = TopicResponse(
            id = topicAggregate.topicId,
            owner = UserData.Public.from(topicAggregate.topic.owner),
            topicName = topicAggregate.topic.topicName,
            description = topicAggregate.topic.description,
            tags = topicAggregate.topic.tags,
            profileImage = topicAggregate.topic.profileImage?.let { AttachmentResponse.create(it) },
            postCount = topicAggregate.postCount,
            followedByCurrentUser = topicAggregate.followerIds.contains(currentUser.id),
            followerCount = topicAggregate.followerCount,
            createdAt = topicAggregate.topic.createdAt,
            updatedAt = topicAggregate.topic.updatedAt,
        )
    }
}
