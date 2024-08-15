package com.kogo.content.service.filehandler

import com.kogo.content.filesystem.FileSystem
import com.kogo.content.util.fixture
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.util.ReflectionTestUtils
import java.io.InputStream
import java.nio.file.Path
import kotlin.test.assertEquals


class LocalStorageFileHandlerServiceTest {

    companion object {
        @TempDir
        lateinit var tempPath: Path
    }

    private val fileSystem : FileSystem = mockk()

    private val localStorageFileHandler : LocalStorageFileHandlerService = LocalStorageFileHandlerService()

    @BeforeEach
    fun setUp(): Unit {
        ReflectionTestUtils.setField(localStorageFileHandler, "mountLocation", tempPath.toString())
        ReflectionTestUtils.setField(localStorageFileHandler, "fileSystemService", fileSystem)
    }

    @Test
    fun `should handle inputstream and store in a file in the filesystem`() {
        val inputStream: InputStream = mockk()
        val metadata: FileMetadata = fixture<FileMetadata>()

        every { fileSystem.createFile(any(), any()) } returns tempPath
        every { fileSystem.write(tempPath, inputStream) } returns Unit
        val result = localStorageFileHandler.store(inputStream, metadata)

        verify (exactly = 1) {
            fileSystem.createFile(any(), any())
            fileSystem.write(tempPath, inputStream)
        }
        assertEquals(result.url, tempPath.toAbsolutePath().toString())
        assertEquals(result.metadata, metadata)
    }

    @Test
    fun `should handle multipart file and store in a file in the filesystem`() {
        val multipartFile = MockMultipartFile("data", "test.txt", "text/plain", "hello world".toByteArray())

        every { fileSystem.createFile(any(), any()) } returns tempPath
        every { fileSystem.write(tempPath, multipartFile) } returns Unit
        val result = localStorageFileHandler.store(multipartFile)

        verify {
            fileSystem.createFile(any(), any())
            fileSystem.write(tempPath, multipartFile)
        }
        assertEquals(result.url, tempPath.toAbsolutePath().toString())
        assertEquals(result.metadata.originalFileName, "test.txt")
        assertEquals(result.metadata.contentType, "text/plain")
    }
}