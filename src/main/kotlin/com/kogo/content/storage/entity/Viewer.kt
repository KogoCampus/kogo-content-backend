package com.kogo.content.storage.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class Viewer (
    @Id
    var id : String? = null,

    var userId: String,

    var viewableId : ObjectId,

    var createdAt: Instant = Instant.now(),
)
