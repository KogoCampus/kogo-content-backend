package com.kogo.content.storage.entity

import com.kogo.content.service.filehandler.FileMetadata
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "profileImages")
data class ProfileImage (
    @Id
    var id: String ?= null,

    var imageUrl: String,

    @Field(name = "meta")
    var metadata: FileMetadata,

    var group: String? = null,
    var user: String? = null,
)