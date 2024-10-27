package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference
import java.time.Instant

@Document(collection = "topics")
@CompoundIndex(def = "{'owner.id': 1}")
data class Topic (
    @Id
    var id : String? = null,

    @Indexed(unique = true)
    var topicName: String,

    var description: String = "",

    @DocumentReference
    var profileImage: Attachment? = null,

    @DocumentReference
    var owner: UserDetails,

    var tags: List<String> = emptyList(),

    var createdAt: Instant?=null,
    var updatedAt: Instant?=null,

    var followingUserCount: Int = 1,
)
