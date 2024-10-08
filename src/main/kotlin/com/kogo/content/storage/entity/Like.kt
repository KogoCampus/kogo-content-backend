package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Like (
    @Id
    var id : String? = null,

    var userId: String,

    var parentId : String
)
