package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class GroupEntity (
    @Id
    var id : String? = null,

    @Indexed(unique = true)
    var groupName: String? = "",

    var userCount: Int? = 0,
) : MongoEntity()