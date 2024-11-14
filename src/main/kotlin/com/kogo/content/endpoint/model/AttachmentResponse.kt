package com.kogo.content.endpoint.model

data class AttachmentResponse (
    val attachmentId: String? = null,
    val name: String,
    val size: Long,
    val contentType: String,
    val url: String
) {
    companion object {
        fun create(attachment: com.kogo.content.storage.entity.Attachment): AttachmentResponse = AttachmentResponse (
            attachmentId = attachment.id,
            name = attachment.name,
            url = attachment.storeKey.toFileSourceUrl(),
            contentType = attachment.contentType,
            size = attachment.fileSize
        )
    }
}
