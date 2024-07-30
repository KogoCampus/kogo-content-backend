package com.kogo.content.storage.entity

import com.kogo.content.service.filehandler.FileMetadata
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Field

data class ProfileImage (
    @Id
    var id : String? = null,

    var imageUrl: String,

    @Field(name = "meta")
    var metadata: FileMetadata,
)