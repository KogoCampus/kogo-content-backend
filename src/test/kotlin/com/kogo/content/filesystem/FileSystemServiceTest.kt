package com.kogo.content.filesystem

import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileSystemServiceTest {

    companion object {
        @TempDir
        lateinit var tempFolder: Path
    }

    private val fileSystemService = FileSystemService(mountLocation = tempFolder.toString())

    @Nested
    inner class `when handle a file` {
        @Test
        fun `should create a file`() {
            fileSystemService.createFile("dummy.txt", "dummy/file/path")
            val path = Paths.get(tempFolder.toString(), "dummy/file/path", "dummy.txt")
            assertTrue { Files.exists(path) }
            assertFalse { Files.isDirectory(path) }
        }

        @Test
        fun `should throw exception if file already exists`() {
            fileSystemService.createFile("dummy.txt", "dummy/file/path")
            assertThrows<LocalStorageException> {
                fileSystemService.createFile("dummy.txt", "dummy/file/path")
            }
        }

        @Test
        fun `should delete a file`() {
            fileSystemService.createFile("dummy.txt", "dummy/file/path")
            val path = Paths.get(tempFolder.toString(), "dummy/file/path", "dummy.txt")
            assertTrue { Files.exists(path) }
            fileSystemService.delete(path.toString())
            assertFalse { Files.exists(path) }
        }
    }

    @Nested
    inner class `when handle a folder` {
        @Test
        fun `should create a folder`() {
            fileSystemService.createFolder("dummy")
            val path = Paths.get(tempFolder.toString(), "dummy")
            Assertions.assertTrue(Files.exists(path))
            Assertions.assertTrue(Files.isDirectory(path))
        }

        @Test
        fun `should throw exception if folder already exists`() {
            fileSystemService.createFolder("dummy")
            assertThrows<LocalStorageException> {
                fileSystemService.createFolder("dummy")
            }
        }

        @Test
        fun `should delete a folder`() {
            fileSystemService.createFolder("dummy", "example/file/path")
            val path = Paths.get(tempFolder.toString(), "example/file/path", "dummy")
            assertTrue(Files.exists(path))
            fileSystemService.delete(path.toString())
            assertFalse { Files.exists(path) }
        }

        @Test
        fun `should delete all files and subdirectories in a folder`() {
            fileSystemService.createFile("dummy1.txt", "example/file/path")
            fileSystemService.createFile("dummy2.txt", "example/file/path")
            fileSystemService.createFile("dummy.txt", "example/file/path/dir")
            fileSystemService.createFile("dummy.txt", "example/file/path/sub/dir")
            val path = Paths.get(tempFolder.toString(), "example/file/path")
            assertTrue { Files.exists(path) }
            assertTrue { Files.exists(path.resolve("dummy1.txt")) }
            assertTrue { Files.exists(path.resolve("dummy2.txt")) }
            assertTrue { Files.exists(path.resolve("dir/dummy.txt")) }
            assertTrue { Files.exists(path.resolve("sub/dir/dummy.txt")) }
            fileSystemService.delete(path.toString())
            assertFalse { Files.exists(path) }
            assertFalse { Files.exists(path.resolve("dummy1.txt")) }
            assertFalse { Files.exists(path.resolve("dummy2.txt")) }
            assertFalse { Files.exists(path.resolve("dir/dummy.txt")) }
            assertFalse { Files.exists(path.resolve("sub/dir/dummy.txt")) }
        }
    }

    @Test
    fun shouldWriteFileFromInputStream() {
        val content = "Dummy inputstream content"
        val inputStream = content.byteInputStream()
        val fileName = "dummy.txt"

        val path = fileSystemService.createFile(fileName)
        fileSystemService.write(path, inputStream)

        val writtenContent = Files.readAllLines(path).joinToString("")
        assertEquals(content, writtenContent)
    }

    @Test
    fun shouldWriteFileFromMultipartFile() {
        val content = "Dummy multipartFile content"
        val multipartFile = MockMultipartFile(
            "file",
            "filename.txt",
            "text/plain",
            content.toByteArray()
        )

        val path = fileSystemService.createFile(multipartFile.originalFilename!!)
        fileSystemService.write(path, multipartFile)

        val writtenContent = Files.readAllLines(path).joinToString("")
        assertEquals(content, writtenContent)
    }

    @AfterEach
    fun tearDown() {
        fileSystemService.delete(tempFolder.toString())
    }
}