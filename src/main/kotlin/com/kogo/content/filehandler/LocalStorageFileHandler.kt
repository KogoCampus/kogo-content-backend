package com.kogo.content.filehandler

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.UUID

@Profile("local | test")
@Component
class LocalStorageFileHandler : FileHandler {

    @Value("\${filehandler.prefix}")
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
            storeKey = FileStoreKey(key = storePath.toAbsolutePath().toString())
        )
    }

    override fun issueFilePublicSourceUrl(storeKey: FileStoreKey): String = storeKey.toString()

    private fun fileStoreLocation(): String {
        val utcDate = ZonedDateTime.now(ZoneOffset.UTC) // Get current date and time in UTC
        return String.format("%d/%d-%d", utcDate.year, utcDate.monthValue, utcDate.dayOfMonth)
    }

    private fun fileStoreName(fileName: String): String {
        return String.format("%s-%s", UUID.randomUUID().toString(), fileName)
    }
}
