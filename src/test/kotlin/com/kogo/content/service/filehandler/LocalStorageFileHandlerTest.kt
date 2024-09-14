package com.kogo.content.service.filehandler

import com.kogo.content.filehandler.LocalStorageFileHandler
import com.kogo.content.filehandler.LocalFileSystem
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


class LocalStorageFileHandlerTest {

    companion object {
        @TempDir
        lateinit var tempPath: Path
    }

    private val localFileSystem : LocalFileSystem = mockk()

    private val localStorageFileHandler : LocalStorageFileHandler = LocalStorageFileHandler()

    @BeforeEach
    fun setUp(): Unit {
        ReflectionTestUtils.setField(localStorageFileHandler, "mountLocation", tempPath.toString())
        ReflectionTestUtils.setField(localStorageFileHandler, "fileSystemService", localFileSystem)
    }
/*
    @Test
    fun `should handle multipart file and store in a file in the filesystem`() {
        val multipartFile = MockMultipartFile("data", "test.txt", "text/plain", "hello world".toByteArray())

        every { localFileSystem.createFile(any(), any()) } returns tempPath
        every { localFileSystem.write(tempPath, multipartFile) } returns Unit
        val result = localStorageFileHandler.store(multipartFile)

        verify {
            localFileSystem.createFile(any(), any())
            localFileSystem.write(tempPath, multipartFile)
        }
        assertEquals(result.url, tempPath.toAbsolutePath().toString())
        assertEquals(result.metadata.originalFileName, "test.txt")
        assertEquals(result.metadata.contentType, "text/plain")
    }
    */
}
