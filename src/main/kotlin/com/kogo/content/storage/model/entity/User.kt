package com.kogo.content.storage.model.entity

import com.fasterxml.jackson.databind.JsonNode
import com.kogo.content.storage.model.Attachment
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference

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

    var followingGroupIds: MutableList<String> = mutableListOf(),

    @DocumentReference
    var blacklistUsers: MutableList<User> = mutableListOf(),

    var friends: MutableList<Friend> = mutableListOf(),

    var appData: AppData = AppData(),

    var latestAccessTimestamp: Long = System.currentTimeMillis(),
    var firstAccessTimestamp: Long = System.currentTimeMillis(),
)

data class SchoolInfo (
    var schoolKey: String,

    var schoolGroupId: String? = null,

    var schoolName: String,

    var schoolShortenedName: String? = null,
)

data class Friend (
    var user: User,

    var nickname: String,

    var status: FriendStatus,

    var createdAt: Long = System.currentTimeMillis(),
) {
    enum class FriendStatus {
        PENDING,
        ACCEPTED,
    }
}

data class AppData(
    var courseSchedule: CourseSchedule? = null,
)

data class CourseSchedule(
    var currentVersion: String,
    var versions: JsonNode,
)
