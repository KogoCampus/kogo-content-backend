package com.kogo.content.storage.repository

import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.Attachment
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Repository
import org.springframework.web.multipart.MultipartFile

@Repository
class AttachmentRepository(
    private val mongoTemplate: MongoTemplate,
    private val fileHandler: FileHandler
) {
    fun save(attachment: Attachment): Attachment {
        return mongoTemplate.save(attachment)
    }

    fun findById(id: String): Attachment? {
        return mongoTemplate.findById(id, Attachment::class.java)
    }

    fun delete(attachment: Attachment) {
        // TODO: Add file deletion from storage
        mongoTemplate.remove(attachment)
    }

    fun saveFile(file: MultipartFile): Attachment {
        val metadata = fileHandler.store(file)
        val attachment = Attachment(
            name = metadata.fileName,
            storeKey = metadata.storeKey,
            contentType = file.contentType!!,
            fileSize = file.size,
        )
        return save(attachment)
    }

    fun saveFiles(files: List<MultipartFile>): List<Attachment> {
        return files.map { saveFile(it) }
    }
}
