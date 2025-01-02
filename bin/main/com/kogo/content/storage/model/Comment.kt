package com.kogo.content.storage.model

import com.kogo.content.storage.model.entity.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.DocumentReference
import java.time.Instant

data class Comment (
    var id: ObjectId = ObjectId(),

    var content: String,

    @DocumentReference
    var author: User,

    var replies: MutableList<Reply> = mutableListOf(),

    // full likes history, regardless currently active or inactive
    var likes: MutableList<Like> = mutableListOf(),

    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    val activeLikes: List<Like>
        get() = likes.filter { it.isActive }
}
