package com.kogo.content.storage.model.entity

import com.kogo.content.storage.model.Attachment
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
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

    var isSchoolGroup: Boolean = false,

    var followers: MutableList<Follower> = mutableListOf(),

    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun isFollowing(user: User) = followers.any { it.follower.id == user.id }
}

data class Follower(
    @DocumentReference
    var follower: User,

    var createdAt: Long = System.currentTimeMillis()
)

