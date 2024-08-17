package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Attachment
import org.springframework.data.mongodb.repository.MongoRepository

interface AttachmentRepository: MongoRepository<Attachment, String> {
}
