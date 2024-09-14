package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

@Document
@CompoundIndex(def = "{'owner.id': 1}")
data class Topic (
    @Id
    var id : String? = null,

    @Indexed(unique = true)
    var topicName: String,

    var description: String = "",

    @DBRef
    var profileImage: Attachment? = null,

    var owner: String,

    var tags: List<String> = emptyList(),

    var userCount: Int = 1,
)
