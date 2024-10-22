package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class Comment (
    @Id
    var id: String? = null,

    @DBRef
    var owner: UserDetails,

    var content: String,
    var parentId: String,
    var parentType: CommentParentType,

    var likes: Int = 0,
    var liked: Boolean = false,
    var createdAt: Instant?=null,

    var repliesCount: Int = 0,
    var replies: List<String>
)

enum class CommentParentType {
    COMMENT,
    POST
}
