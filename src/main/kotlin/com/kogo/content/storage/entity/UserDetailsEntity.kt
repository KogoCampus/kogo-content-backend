package com.kogo.content.storage.entity

import com.fasterxml.jackson.annotation.JsonManagedReference
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class UserDetailsEntity (
    @Id
    var id: String? = null,

    @Indexed(unique = true)
    var username: String,

    var email: String? = null,

    var schoolId: String? = null,

    @DBRef
    var profileImage: Attachment? = null,

    @DBRef
    @JsonManagedReference
    var followingTopics: List<TopicEntity>? = emptyList()
)
