package com.kogo.content.storage.entity

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.validation.constraints.NotBlank
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class StudentUserEntity (
    @Id
    var id: String? = null,

    @field:NotBlank
    @Indexed(unique = true)
    var username: String,

    @field:NotBlank
    var email: String,

    @field:NotBlank
    var schoolId: String,

    @DBRef
    var profileImage: Attachment? = null,

    @DBRef
    @JsonManagedReference
    var followingTopics: List<TopicEntity>? = emptyList()
)
