package com.kogo.content.service.filehandler

import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

interface FileHandlerService {
    fun store(content: InputStream, metadata: FileMetadata): FileStoreResult

    fun store(content: MultipartFile): FileStoreResult
}