package com.kogo.content.filehandler

import org.springframework.web.multipart.MultipartFile

interface FileHandler {
    fun store(content: MultipartFile): FileStoreMetadata

    fun getFileReadUrl(storeKey: FileStoreKey): String
}
