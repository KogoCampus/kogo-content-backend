package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.UserDetails


data class UserInfo (
    var id: String,
    var username: String,
    var email: String,
    var profileImage: AttachmentResponse?= null,
    var schoolName: String,
    var schoolShortenedName: String
) {
    companion object {
        fun from(user: UserDetails) = UserInfo(
            id = user.id!!,
            username = user.username,
            email = user.email!!,
            profileImage = user.profileImage?.let { AttachmentResponse.from(it) },
            schoolName = user.schoolName!!,
            schoolShortenedName = user.schoolShortenedName!!
        )
    }
}
