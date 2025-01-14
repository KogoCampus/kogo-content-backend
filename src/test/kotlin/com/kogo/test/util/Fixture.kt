package com.kogo.test.util

import com.kogo.content.storage.model.Attachment
import com.kogo.content.storage.model.Comment
import com.kogo.content.storage.model.Like
import com.kogo.content.storage.model.Reply
import com.kogo.content.storage.model.entity.*
import org.bson.types.ObjectId

class Fixture {
    companion object {
        private fun generateObjectIdString() = ObjectId().toString()

        fun createUserFixture(
            id: String = generateObjectIdString(),
            username: String = "user-$id",
            email: String = "user-$id@example.com",
            blacklist: MutableSet<Pair<BlacklistItem, String>> = mutableSetOf()
        ) = User(
            id = id,
            username = username,
            email = email,
            schoolInfo = SchoolInfo(
                schoolName = "Test School",
                schoolKey = "TEST",
                schoolShortenedName = "TS"
            ),
            blacklist = blacklist
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
            followers = mutableListOf(Follower(owner)),
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

        fun createCommentFixture(
            id: String = generateObjectIdString(),
            author: User = createUserFixture(),
            content: String = "comment-$id",
            likes: MutableList<Like> = mutableListOf(),
            replies: MutableList<Reply> = mutableListOf()
        ) = Comment(
            id = id,
            author = author,
            content = content,
            likes = likes,
            replies = replies
        )

        fun createReplyFixture(
            id: String = generateObjectIdString(),
            author: User = createUserFixture(),
            content: String = "reply-$id",
            likes: MutableList<Like> = mutableListOf()
        ) = Reply(
            id = id,
            author = author,
            content = content,
            likes = likes
        )
    }
}
