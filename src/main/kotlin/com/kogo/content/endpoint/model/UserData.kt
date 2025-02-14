package com.kogo.content.endpoint.model

import com.kogo.content.storage.model.entity.Friend
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User

class UserData {
    data class IncludeCredentials (
        var id: String,
        var username: String,
        var email: String,
        var profileImage: AttachmentResponse? = null,
        var schoolInfo: SchoolInfo,
        var appLocalData: String,
        var friends: List<FriendData>,
        var blacklistUsers: List<Public>,
    ) {
        companion object {
            fun from(user: User) = IncludeCredentials(
                id = user.id!!,
                username = user.username,
                email = user.email,
                profileImage = user.profileImage?.let { AttachmentResponse.from(it) },
                schoolInfo = user.schoolInfo,
                blacklistUsers = user.blacklistUsers.map { Public.from(it) },
                appLocalData = user.appLocalData ?: "",
                friends = user.friends.filter { it.status == Friend.FriendStatus.ACCEPTED }
                    .map { FriendData(
                        nickname = it.nickname,
                        friendUserId = it.user.id!!,
                        appLocalData = it.user.appLocalData ?: "",
                    ) }
            )
        }

        data class FriendData (
            var nickname: String,
            var friendUserId: String,
            var appLocalData: String,
        )
    }

    data class Public(
        val id: String,
        val username: String,
        var profileImage: AttachmentResponse? = null,
        val schoolName: String,
        val schoolShortenedName: String? = null,
    ) {
        companion object {
            fun from(user: User): Public {
                return Public(
                    id = user.id!!,
                    username = user.username,
                    profileImage = user.profileImage?.let { AttachmentResponse.from(it) },
                    schoolName = user.schoolInfo.schoolName,
                    schoolShortenedName = user.schoolInfo.schoolShortenedName
                )
            }
        }
    }
}
