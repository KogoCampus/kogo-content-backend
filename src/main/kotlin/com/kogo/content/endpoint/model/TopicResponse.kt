package com.kogo.content.endpoint.model

import org.springframework.web.multipart.MultipartFile

data class TopicResponse(
    var id: String,
    var ownerId: String? = null,
    var topicName: String,
    var description: String,
    var tags: List<String> = emptyList(),
    var profileImage: TopicProfileImage? = null
) {
    data class TopicProfileImage (
        val attachmentId: String,
        val fileName: String,
        val size: Long,
        val contentType: String,
        val url: String
    )
}
