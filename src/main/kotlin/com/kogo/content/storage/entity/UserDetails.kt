package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference

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

    @DocumentReference
    var profileImage: Attachment? = null,
)
