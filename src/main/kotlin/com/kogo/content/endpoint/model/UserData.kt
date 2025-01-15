package com.kogo.content.endpoint.model

import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User

class UserData {
    data class IncludeCredentials (
        var id: String,
        var username: String,
        var email: String,
        var profileImage: AttachmentResponse? = null,
        var schoolInfo: SchoolInfo,
        var pushNotificationToken: String?,
    ) {
        companion object {
            fun from(user: User) = IncludeCredentials(
                id = user.id!!,
                username = user.username,
                email = user.email,
                profileImage = user.profileImage?.let { AttachmentResponse.from(it) },
                schoolInfo = user.schoolInfo,
                pushNotificationToken = user.pushNotificationToken,
            )
        }
    }

    data class Public(
        val id: String,
        val username: String,
        var profileImage: AttachmentResponse? = null,
        val schoolName: String,
        val schoolShortenedName: String,
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
