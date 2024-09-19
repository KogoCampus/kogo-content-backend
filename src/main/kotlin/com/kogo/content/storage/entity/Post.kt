package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

@Document
@CompoundIndex(def = "{'author.id': 1}")
data class Post (
    @Id
    var id : String? = null,

    var title: String = "",

    @DBRef
    var topic: Topic,

    @DBRef
    var author: UserDetails,

    var content: String = "",

    @DBRef
    var attachments: List<Attachment> = emptyList(),

    // TODO: comment entity ref
    var comments: List<String> = emptyList(),

    var viewcount: Int = 0,
    var likes: Int = 0,
)
