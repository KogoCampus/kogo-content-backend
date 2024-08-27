package com.kogo.content.service

import com.kogo.content.storage.entity.Attachment
import com.kogo.content.service.exception.UnsupportedMediaTypeException
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.service.filehandler.FileHandlerService
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.MediaType

@Service
class AttachmentService(
    private val attachmentRepository: AttachmentRepository,
    private val fileHandler: FileHandlerService
) {
    fun saveAttachment(file: MultipartFile, entityId: String?, entityType: String) : Attachment {
        val acceptedMediaTypes = arrayOf(
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_JPEG_VALUE
        )
        file.takeIf { it.contentType in acceptedMediaTypes } ?: throw with(file) {
            val errorMessage = String.format("Invalid media type for profile image. Accepted types are: %s, but provided: %s.",
                acceptedMediaTypes.toString(), contentType)
            UnsupportedMediaTypeException(errorMessage)
        }
        val storeResult = fileHandler.store(file)
        val attachment = Attachment(
            imageUrl = storeResult.url,
            metadata = storeResult.metadata,
        )
        when (entityType.lowercase()) {
            "post" -> attachment.post = entityId
            "group" -> attachment.group = entityId
            "user" -> attachment.user = entityId
            else -> throw IllegalArgumentException("Unsupported entity type: $entityType")
        }
        return attachmentRepository.save(attachment)
    }
}