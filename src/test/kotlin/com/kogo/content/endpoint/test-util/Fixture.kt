package com.kogo.content.endpoint.`test-util`

import com.kogo.content.storage.model.Attachment
import com.kogo.content.storage.model.Comment
import com.kogo.content.storage.model.Like
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.Post
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import org.bson.types.ObjectId

class Fixture {
    companion object {
        private fun generateObjectIdString() = ObjectId().toString()

        fun createUserFixture(
            id: String = generateObjectIdString(),
            username: String = "user-$id",
            email: String = "user-$id@example.com"
        ) = User(
            id = id,
            username = username,
            email = email,
            schoolInfo = SchoolInfo(
                schoolName = "Test School",
                schoolKey = "TEST",
                schoolShortenedName = "TS"
            )
        )

        fun createGroupFixture(
            id: String = generateObjectIdString(),
            name: String = "group-$id",
            description: String = "description-$id",
            owner: User = createUserFixture(),
            followerIds: MutableList<String> = mutableListOf()
        ) = Group(
            id = id,
            groupName = name,
            description = description,
            owner = owner,
            followerIds = followerIds.apply { add(owner.id!!) }
        )

        fun createPostFixture(
            id: String = generateObjectIdString(),
            group: Group,
            author: User = createUserFixture(),
            title: String = "post-$id",
            content: String = "content-$id",
            attachments: MutableList<Attachment> = mutableListOf(),
            likes: MutableList<Like> = mutableListOf(),
            viewerIds: MutableList<String> = mutableListOf(),
            comments: MutableList<Comment> = mutableListOf(),
        ) = Post(
            id = id,
            group = group,
            author = author,
            title = title,
            content = content,
            attachments = attachments,
            likes = likes,
            viewerIds = viewerIds,
            comments = comments,
        )
    }
}
