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
            name = metadata.fileName,
            storeKey = metadata.storeKey,
            contentType = file.contentType!!,
            fileSize = file.size,
        )
        attachmentRepository.save(attachment)
        return attachment
    }
}
