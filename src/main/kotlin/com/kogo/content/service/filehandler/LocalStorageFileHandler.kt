package com.kogo.content.service.filehandler

import com.kogo.content.filesystem.FileSystemService
import com.kogo.content.filesystem.LocalStorageException
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.Calendar
import java.util.UUID


@Component
class LocalStorageFileHandler : FileHandler {

    @Value("\${filehandler.local.mountLocation}")
    lateinit var mountLocation: String

    lateinit var fileSystemService: FileSystemService

    @PostConstruct
    fun init() {
        fileSystemService = FileSystemService(mountLocation)
    }

    override fun store(content: InputStream, metadata: FileMetadata): FileStoreResult {
        val fileStoreLocation = fileStoreLocation()
        val fileStoreName = fileStoreName(metadata.originalFileName ?: "")
        val storePath = fileSystemService.createFile(fileStoreName, fileStoreLocation)
        fileSystemService.write(storePath, content)
        return FileStoreResult(
            url = storePath.toAbsolutePath().toString(),
            fileName = fileStoreName,
            metadata = metadata
        )
    }

    override fun store(content: MultipartFile): FileStoreResult {
        val metadata = FileMetadata.from(content)
        val fileStoreLocation = fileStoreLocation()
        val fileStoreName = fileStoreName(metadata.originalFileName ?: "")
        val storePath = fileSystemService.createFile(fileStoreName, fileStoreLocation)
        fileSystemService.write(storePath, content)
        return FileStoreResult(
            url = storePath.toAbsolutePath().toString(),
            fileName = fileStoreName,
            metadata = metadata
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