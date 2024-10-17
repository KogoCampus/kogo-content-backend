package com.kogo.content.endpoint.model


data class OwnerInfoResponse(
    var ownerId: String? = null,
    var username: String,
    var profileImage: AttachmentResponse?= null,
    var schoolShortenedName: String? = null
)
