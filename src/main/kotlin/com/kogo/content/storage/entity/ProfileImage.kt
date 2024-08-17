package com.kogo.content.storage.entity

import com.kogo.content.service.filehandler.FileMetadata
import org.springframework.data.mongodb.core.mapping.Field

data class ProfileImage (
    var imageUrl: String,

    @Field(name = "meta")
    var metadata: FileMetadata,
)