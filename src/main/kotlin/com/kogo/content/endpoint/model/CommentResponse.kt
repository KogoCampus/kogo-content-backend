package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.CommentParentType
import java.time.Instant

data class CommentResponse (
    var id: String,
    var authorId: String?,
    var content: String,
    var parentId: String,
    var parentType: CommentParentType,
    val likes: Int,
    val liked: Boolean,
    var createdAt: Instant
) {}
