package com.kogo.content.storage.model

import org.bson.types.ObjectId

data class Attachment(
    var id: String,

    var filename: String,

    var url: String,

    var contentType: String,

    var size: Long, // Size in Bytes
)
