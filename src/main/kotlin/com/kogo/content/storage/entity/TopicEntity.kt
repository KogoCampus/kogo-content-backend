package com.kogo.content.storage.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

@Document
@CompoundIndex(def = "{'owner.id': 1}")
data class TopicEntity (
    @Id
    var id : String? = null,

    @Indexed(unique = true)
    var topicName: String,

    var userCount: Int = 1,

    @DBRef
    var profileImage: Attachment?,

    @DBRef
    @JsonBackReference
    var owner: StudentUserEntity?,

    var description: String = "",

    var tags: List<String> = emptyList()

)
