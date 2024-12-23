package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.User


class UserData {
    data class IncludeCredentials (
        var id: String,
        var pushToken: String,
        var username: String,
        var email: String,
        var profileImage: AttachmentResponse?= null,
        var schoolName: String,
        var schoolShortenedName: String
    ) {
        companion object {
            fun from(user: User) = IncludeCredentials(
                id = user.id!!,
                pushToken = user.pushToken.toString(),
                username = user.username,
                email = user.email!!,
                profileImage = user.profileImage?.let { AttachmentResponse.create(it) },
                schoolName = user.schoolName!!,
                schoolShortenedName = user.schoolShortenedName!!
            )
        }
    }

    data class Public(
        val id: String,
        val username: String,
        var profileImage: AttachmentResponse?= null,
        val schoolName: String,
        val schoolShortenedName: String,
    ) {
        companion object {
            fun from(user: User) = Public(
                id = user.id!!,
                username = user.username,
                profileImage = user.profileImage?.let { AttachmentResponse.create(it) },
                schoolName = user.schoolName!!,
                schoolShortenedName = user.schoolShortenedName!!
            )
        }
    }
}
