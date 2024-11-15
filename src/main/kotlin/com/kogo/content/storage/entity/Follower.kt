package com.kogo.content.storage.entity

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.annotation.Id
import java.time.Instant

@Document
data class Follower(
    @Id
    var id: String? = null,

    var userId: String,

    var followableId: ObjectId,

    var createdAt: Instant = Instant.now(),
) {}
