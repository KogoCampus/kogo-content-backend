package com.kogo.content.endpoint.model

import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.User
import java.time.Instant

data class GroupResponse(
    var id: String,
    var groupName: String,
    var description: String,
    var owner: UserData.Public,
    var tags: List<String> = emptyList(),
    var profileImage: AttachmentResponse? = null,
    var followerCount: Int,
    var followedByCurrentUser: Boolean,
    var createdAt: Instant,
    var updatedAt: Instant,
) {
    companion object {
        fun from(group: Group, currentUser: User) = GroupResponse(
            id = group.id!!,
            owner = UserData.Public.from(group.owner),
            groupName = group.groupName,
            description = group.description,
            tags = group.tags,
            profileImage = group.profileImage?.let { AttachmentResponse.from(it) },
            followedByCurrentUser = group.followerIds.contains(currentUser.id),
            followerCount = group.followerIds.size,
            createdAt = group.createdAt,
            updatedAt = group.updatedAt,
        )
    }
}
