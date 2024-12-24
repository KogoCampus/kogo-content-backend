package com.kogo.content.storage.model.entity

import com.kogo.content.storage.model.Attachment
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference
import java.time.Instant

@Document
data class Group (
    @Id
    var id : String? = null,

    @Indexed(unique = true)
    var groupName: String,

    var description: String = "",

    var profileImage: Attachment? = null,

    var tags: MutableList<String> = mutableListOf(),

    @DocumentReference
    var owner: User,

    var followerIds: MutableList<String> = mutableListOf(),

    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
)
