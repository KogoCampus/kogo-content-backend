package com.kogo.content.storage.entity

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.annotation.Id
import java.time.Instant

@Document
data class UserFollowing(
    @Id
    var id: String? = null,

    var userId: String,

    var followableId: String,

    var createdAt: Instant = Instant.now(),
) {}
