package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class View (
    @Id
    var id : String? = null,

    var userId: String,

    var viewableId : String
)
