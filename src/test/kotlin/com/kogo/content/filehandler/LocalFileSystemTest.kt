package com.kogo.content.filehandler

import java.io.IOException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalFileSystemTest {

    companion object {
        @TempDir
        lateinit var tempFolder: Path
    }

    private val localFileSystem = LocalFileSystem(mountLocation = tempFolder.toString())

    @Nested
    inner class `when handle a file` {
        @Test
        fun `should create a file`() {
            localFileSystem.createFile("dummy.txt", "dummy/file/path")
            val path = Paths.get(tempFolder.toString(), "dummy/file/path", "dummy.txt")
            assertTrue { Files.exists(path) }
            assertFalse { Files.isDirectory(path) }
        }

        @Test
        fun `should throw exception if file already exists`() {
            localFileSystem.createFile("dummy.txt", "dummy/file/path")
            assertThrows<IOException> {
                localFileSystem.createFile("dummy.txt", "dummy/file/path")
            }
        }

        @Test
        fun `should delete a file`() {
            localFileSystem.createFile("dummy.txt", "dummy/file/path")
            val path = Paths.get(tempFolder.toString(), "dummy/file/path", "dummy.txt")
            assertTrue { Files.exists(path) }
            localFileSystem.delete(path.toString())
            assertFalse { Files.exists(path) }
        }
    }

    @Nested
    inner class `when handle a folder` {
        @Test
        fun `should create a folder`() {
            localFileSystem.createFolder("dummy")
            val path = Paths.get(tempFolder.toString(), "dummy")
            Assertions.assertTrue(Files.exists(path))
            Assertions.assertTrue(Files.isDirectory(path))
        }

        @Test
        fun `should throw exception if folder already exists`() {
            localFileSystem.createFolder("dummy")
            assertThrows<IOException> {
                localFileSystem.createFolder("dummy")
            }
        }

        @Test
        fun `should delete a folder`() {
            localFileSystem.createFolder("dummy", "example/file/path")
            val path = Paths.get(tempFolder.toString(), "example/file/path", "dummy")
            assertTrue(Files.exists(path))
            localFileSystem.delete(path.toString())
            assertFalse { Files.exists(path) }
        }

        @Test
        fun `should delete all files and subdirectories in a folder`() {
            localFileSystem.createFile("dummy1.txt", "example/file/path")
            localFileSystem.createFile("dummy2.txt", "example/file/path")
            localFileSystem.createFile("dummy.txt", "example/file/path/dir")
            localFileSystem.createFile("dummy.txt", "example/file/path/sub/dir")
            val path = Paths.get(tempFolder.toString(), "example/file/path")
            assertTrue { Files.exists(path) }
            assertTrue { Files.exists(path.resolve("dummy1.txt")) }
            assertTrue { Files.exists(path.resolve("dummy2.txt")) }
            assertTrue { Files.exists(path.resolve("dir/dummy.txt")) }
            assertTrue { Files.exists(path.resolve("sub/dir/dummy.txt")) }
            localFileSystem.delete(path.toString())
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

        val path = localFileSystem.createFile(fileName)
        localFileSystem.write(path, inputStream)

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

        val path = localFileSystem.createFile(multipartFile.originalFilename!!)
        localFileSystem.write(path, multipartFile)

        val writtenContent = Files.readAllLines(path).joinToString("")
        assertEquals(content, writtenContent)
    }

    @AfterEach
    fun tearDown() {
        localFileSystem.delete(tempFolder.toString())
    }
}
