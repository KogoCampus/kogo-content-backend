package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class View (
    @Id
    var id : String? = null,

    var userId: String,

    var viewableId : String,

    var createdAt: Instant = Instant.now(),
)
