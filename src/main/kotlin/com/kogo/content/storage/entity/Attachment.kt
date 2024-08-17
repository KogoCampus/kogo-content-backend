package com.kogo.content.storage.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIgnore
import com.kogo.content.service.filehandler.FileMetadata
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DBRef

@Document(collection = "attachments")
data class Attachment(
    @Id
    var id: String ?= null,

    var imageUrl: String,

    @Field(name = "meta")
    var metadata: FileMetadata,

    var post: String? = null,
)