package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference
import java.time.Instant

@Document(collection = "posts")
data class Post (
    @Id
    var id : String? = null,

    var title: String = "",

    @DocumentReference
    var topic: Topic,

    @DocumentReference
    var author: UserDetails,

    var content: String = "",

    @DocumentReference
    var attachments: List<Attachment> = emptyList(),

    var viewCount: Int = 0,
    var commentCount: Int = 0,
    
    var likes: Int = 0,

    var createdAt: Instant?=null,
    var updatedAt: Instant?=null,
)
