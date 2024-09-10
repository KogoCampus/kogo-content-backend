package com.kogo.content.storage.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIgnore
import com.kogo.content.filehandler.FileStoreMetadata
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

@Document(collection = "attachments")
data class Attachment(
    @Id
    var id: String? = null,

    var fileName: String,

    var savedLocationURL: String,

    var contentType: String,

    var fileSize: Long,
)
