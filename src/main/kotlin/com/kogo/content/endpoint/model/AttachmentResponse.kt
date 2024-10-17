package com.kogo.content.endpoint.model

data class AttachmentResponse (
    val attachmentId: String? = null,
    val name: String,
    val size: Long,
    val contentType: String,
    val url: String
)
