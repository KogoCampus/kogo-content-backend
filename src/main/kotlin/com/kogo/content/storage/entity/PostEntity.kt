package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

@Document
@CompoundIndex(def = "{'author.id': 1}")
data class PostEntity (
    @Id
    var id : String? = null,

    var title: String = "",
    var content: String = "",

    var comments: List<String> = emptyList(),
    var commentCount: Int = 0,

    @DBRef
    var author: StudentUserEntity?= null,
    @DBRef
    var attachments: List<Attachment>? = emptyList(),
    @DBRef
    var group: TopicEntity ?= null,

    var viewed: Boolean = false,
    var liked: Boolean = false,
) : MongoEntity()