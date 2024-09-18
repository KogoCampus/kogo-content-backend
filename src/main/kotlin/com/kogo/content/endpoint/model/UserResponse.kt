package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.Topic

data class UserResponse (
    var id: String,
    var username: String? = null,
    var email: String? = null,
    var schoolId: String? = null,
    var profileImage: UserProfileImage? = null,
    var followingTopics: List<Topic>? = emptyList()
) {
    data class UserProfileImage (
        val attachmentId: String,
        val fileName: String,
        val size: Long,
        val contentType: String,
        val url: String
    )
}


