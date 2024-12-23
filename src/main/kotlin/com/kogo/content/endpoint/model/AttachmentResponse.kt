package com.kogo.content.endpoint.model

import com.kogo.content.storage.model.Attachment

data class AttachmentResponse (
    val id: String,
    val name: String,
    val url: String,
    val size: Long,
    val contentType: String,
) {
    companion object {
        fun from(attachment: Attachment): AttachmentResponse {
            return AttachmentResponse(
                id = attachment.id.toString(),
                name = attachment.filename,
                url = attachment.url,
                size = attachment.size,
                contentType = attachment.contentType
            )
        }
    }
}
