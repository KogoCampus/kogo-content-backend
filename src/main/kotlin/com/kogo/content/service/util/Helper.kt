package com.kogo.content.service.util

import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.repository.AttachmentRepository
import org.springframework.web.multipart.MultipartFile

fun saveFileAndConvertToAttachment(file: MultipartFile, fileHandler: FileHandler, attachmentRepository: AttachmentRepository): Attachment {
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

fun deleteAttachment(attachment: Attachment, attachmentRepository: AttachmentRepository) {
    attachmentRepository.deleteById(attachment.id!!)
}
