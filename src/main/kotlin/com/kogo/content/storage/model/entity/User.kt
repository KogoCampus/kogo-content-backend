package com.kogo.content.storage.model.entity

import com.kogo.content.storage.model.Attachment
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class User (
    @Id
    var id: String? = null,

    @Indexed(unique = true)
    var username: String,

    var email: String,

    var schoolInfo: SchoolInfo,

    var pushNotificationToken: String? = null,

    var profileImage: Attachment? = null,

    var blacklist: MutableSet<Pair<BlacklistItem, String>> = mutableSetOf(),

    val followingGroupIds: MutableList<String> = mutableListOf(),
) {
    fun blacklistedItemIds(itemType: BlacklistItem) = blacklist.filter { it.first == itemType }.map { it.second }
}

data class SchoolInfo (
    var schoolKey: String,

    var schoolGroupId: String? = null,

    var schoolName: String,

    var schoolShortenedName: String,
)

enum class BlacklistItem {
    Group,
    Post,
    Comment,
    User
}
