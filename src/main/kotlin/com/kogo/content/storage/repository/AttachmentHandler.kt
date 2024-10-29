package com.kogo.content.storage.repository

import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.Attachment
import org.springframework.web.multipart.MultipartFile

interface AttachmentHandler {
    fun saveFileAndReturnAttachment(file: MultipartFile, fileHandler: FileHandler, attachmentRepository: AttachmentRepository): Attachment
}
