package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "likes")
data class Like (
    @Id
    var id : String? = null,

    var userId: String,

    var likableId : String
)
