package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference
import java.time.Instant

@Document(collection = "comments")
data class Comment (
    @Id
    var id: String? = null,

    var content: String,

    @DocumentReference
    var author: UserDetails,

    @DocumentReference
    var post: Post,

    var replyCount: Int = 0,

    var likes: Int = 0,

    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
)
