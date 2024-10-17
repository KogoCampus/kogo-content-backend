package com.kogo.content.endpoint.model

import java.time.Instant

data class TopicResponse(
    var id: String,
    var owner: OwnerInfoResponse,
    var topicName: String,
    var description: String,
    var tags: List<String> = emptyList(),
    var profileImage: AttachmentResponse? = null,
    var createdAt: Instant
) {}
