package com.kogo.content.storage.entity

import com.fasterxml.jackson.annotation.JsonManagedReference
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class UserDetails (
    @Id
    var id: String? = null,

    @Indexed(unique = true)
    var idToken: UserIdToken? = null,

    @Indexed(unique = true)
    var username: String,

    var email: String? = null,

    var schoolName: String? = null,

    var schoolShortenedName: String? = null,

    @DBRef
    var profileImage: Attachment? = null,
)
