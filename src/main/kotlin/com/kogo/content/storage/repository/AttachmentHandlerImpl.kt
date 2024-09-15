package com.kogo.content.storage.repository

import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.Attachment
import org.springframework.web.multipart.MultipartFile

class AttachmentHandlerImpl : AttachmentHandler {
    override fun saveFileAndReturnAttachment(
        file: MultipartFile,
        fileHandler: FileHandler,
        attachmentRepository: AttachmentRepository
    ): Attachment {
        val metadata = fileHandler.store(file)
        val attachment = Attachment(
            fileName = metadata.fileName,
            savedLocationURL = metadata.url,
            contentType = metadata.contentType,
            fileSize = metadata.size,
        )
        attachmentRepository.save(attachment)
        return attachment
    }
}
