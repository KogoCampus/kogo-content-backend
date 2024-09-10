package com.kogo.content.filehandler

data class FileStoreMetadata (
    var fileName: String,
    var url: String,
    var contentType: String,
    var size: Long
)
