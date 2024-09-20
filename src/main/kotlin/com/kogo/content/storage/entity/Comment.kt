package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

@Document
@CompoundIndex(def = "{'author.id': 1}")
data class Comment (
    @Id
    var id: String? = null,

    @DBRef
    var author: UserDetails,
    var content: String,
    var parentId: String,
    var parentType: CommentParentType,

    var likes: Int = 0,
    var liked: Boolean = false,

    var repliesCount: Int = 0
)

enum class CommentParentType {
    COMMENT,
    POST
}
