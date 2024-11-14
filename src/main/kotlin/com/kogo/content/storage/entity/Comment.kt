package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference
import java.time.Instant

@Document
data class Comment (
    @Id
    var id: String? = null,

    var content: String,

    @DocumentReference
    var author: User,

    @DocumentReference
    var post: Post,

    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
)
