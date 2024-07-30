package com.kogo.content.service.filehandler

import org.springframework.web.multipart.MultipartFile

data class FileMetadata (
    var originalFileName: String?,
    var contentType: String?,
    var size: Long?
) {
    companion object {
        fun from(multipartFile: MultipartFile): FileMetadata {
            return FileMetadata(
                originalFileName = multipartFile.originalFilename,
                contentType = multipartFile.contentType,
                size = multipartFile.size,
            )
        }
    }
}