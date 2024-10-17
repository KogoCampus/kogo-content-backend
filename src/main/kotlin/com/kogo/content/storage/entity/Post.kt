package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class Post (
    @Id
    var id : String? = null,

    var title: String = "",

    @DBRef
    var topic: Topic,

    @DBRef
    var owner: UserDetails,

    var content: String = "",

    @DBRef
    var attachments: List<Attachment> = emptyList(),

    @DBRef
    var comments: List<Comment>,

    var commentCount: Int = 0,
    var createdAt: Instant?=null,

    var viewcount: Int = 0,
    var likes: Int = 0,
)
