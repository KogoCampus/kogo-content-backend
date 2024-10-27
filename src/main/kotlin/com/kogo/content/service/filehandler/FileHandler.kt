package com.kogo.content.service.filehandler

import com.kogo.content.filesystem.FileStoreKey
import com.kogo.content.filesystem.FileStoreMetadata
import org.springframework.web.multipart.MultipartFile

interface FileHandler {
    fun store(content: MultipartFile): FileStoreMetadata

    fun issueFilePublicSourceUrl(storeKey: FileStoreKey): String
}
