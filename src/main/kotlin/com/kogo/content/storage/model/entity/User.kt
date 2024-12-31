package com.kogo.content.storage.model.entity

import com.kogo.content.storage.model.Attachment
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class User (
    @Id
    var id: String? = null,

    @Indexed(unique = true)
    var username: String,

    var email: String,

    var schoolInfo: SchoolInfo,

    var pushNotificationToken: String? = null,

    var profileImage: Attachment? = null,

    val followingGroupIds: MutableList<String> = mutableListOf(),
)

data class SchoolInfo (
    var schoolKey: String,

    var schoolGroupId: String? = null,

    var schoolName: String,

    var schoolShortenedName: String,
)
