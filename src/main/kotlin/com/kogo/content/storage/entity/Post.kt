package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference
import java.time.Instant

@Document
data class Post (
    @Id
    var id : String? = null,

    var title: String,

    var content: String,

    @DocumentReference
    var topic: Topic,

    @DocumentReference
    var author: User,

    var attachments: List<Attachment> = emptyList(),

    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
)
