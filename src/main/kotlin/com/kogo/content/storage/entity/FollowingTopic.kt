package com.kogo.content.storage.entity

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef

@Document
data class FollowingTopic(
    @Id
    var id: String? = null,

    var userId: String,

    var topicId: String
) {}
