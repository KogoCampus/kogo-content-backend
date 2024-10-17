package com.kogo.content.endpoint.model

import com.kogo.content.storage.entity.Topic

data class UserResponse (
    var id: String,
    var username: String? = null,
    var email: String? = null,
    var schoolName: String? = null,
    var schoolShortenedName: String? = null,
    var profileImage: AttachmentResponse? = null
) {}


