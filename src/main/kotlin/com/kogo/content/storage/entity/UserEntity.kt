package com.kogo.content.storage.entity

import com.fasterxml.jackson.annotation.JsonManagedReference
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class UserEntity (
    @Id
    var id: String? = null,

    var username: String? = "",

    var email: String? = "",

    @DBRef
    var profileImage: Attachment? = null,

    @DBRef
    @JsonManagedReference
    var following: List<GroupEntity>? = emptyList()
) : MongoEntity()