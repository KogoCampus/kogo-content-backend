package com.kogo.content.service

import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.TopicRepository
import org.springframework.stereotype.Service

@Service
class SearchIndexService (
    private val repository: TopicRepository,
    private val attachmentRepository: AttachmentRepository,
    private val fileHandler: FileHandler
) {

}
