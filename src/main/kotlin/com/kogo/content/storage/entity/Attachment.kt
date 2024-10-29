package com.kogo.content.storage.entity

import com.kogo.content.filehandler.FileStoreKey
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Attachment(
    @Id
    var id: String? = null,

    var name: String,

    var storeKey: FileStoreKey,

    var contentType: String,

    var fileSize: Long,
)
