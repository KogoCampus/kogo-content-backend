package com.kogo.content.filehandler

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.Calendar
import java.util.UUID


@Component
class LocalStorageFileHandler : FileHandler {

    @Value("\${filehandler.local.mountLocation}")
    lateinit var mountLocation: String

    lateinit var localFileSystem: LocalFileSystem

    @PostConstruct
    fun init() {
        localFileSystem = LocalFileSystem(mountLocation)
    }

    override fun store(content: MultipartFile): FileStoreMetadata {
        val fileStoreLocation = fileStoreLocation()
        val fileStoreName = fileStoreName(content.originalFilename ?: "")
        val storePath = localFileSystem.createFile(fileStoreName, fileStoreLocation)
        localFileSystem.write(storePath, content)
        return FileStoreMetadata(
            fileName = fileStoreName,
            url = storePath.toAbsolutePath().toString(),
            contentType = content.contentType!!,
            size = content.size
        )
    }

    private fun fileStoreLocation(): String {
        val calendar: Calendar = Calendar.getInstance()
        return String.format("%s/%s-%s", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    }

    private fun fileStoreName(fileName: String): String {
        return String.format("%s-%s", UUID.randomUUID().toString(), fileName)
    }
}
