package com.kogo.content.filesystem

import com.kogo.content.logging.Logger
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class FileSystemService (
    private val mountLocation: String
) {
    companion object : Logger()

    fun createFile(fileName: String, filepath: String = ""): Path {
        val path = Paths.get(mountLocation, filepath, fileName)
        checkFileExists(path)
        try {
            log.debug { String.format("Creating a file; path=%s;", path) }
            if (!Files.exists(path.parent)) Files.createDirectories(path.parent)
            Files.createFile(path)
        } catch (e: IOException) {
            val errorMessage = String.format("Failed to create a file; path=%s;", path)
            throw LocalStorageException(errorMessage, e)
        }
        return path
    }

    fun createFolder(folderName: String, folderPath: String = "") {
        val path = Paths.get(mountLocation, folderPath, folderName)
        checkFileExists(path)
        try {
            log.debug { String.format("Creating a directory; path=%s;", path) }
            if (!Files.exists(path.parent)) Files.createDirectories(path.parent)
            Files.createDirectory(path)
        } catch (e: IOException) {
            val errorMessage = String.format("Failed to create a directory; path=%s;", path)
            throw LocalStorageException(errorMessage, e)
        }
    }

    /**
     * Only capable of deleting a file or an empty directory
     */
    fun safeDelete(path: String) {
        val path = Path.of(path)
        if (!Files.exists(path)) {
            val errorMessage = String.format("Failed to delete; Path not found; path=%s;", path)
            log.error { errorMessage }
            throw LocalStorageException(errorMessage)
        }
        try {
            Files.delete(path)
        } catch (e: IOException) {
            val errorMessage = String.format("Failed to delete; path=%s", path)
            throw LocalStorageException(errorMessage, e)
        }
    }

    /**
     * Deleting a file or a directory with files and subdirectories in it recursively
     */
    fun delete(path: String) {
        val path = Path.of(path)
        if (!Files.exists(path)) {
            val errorMessage = String.format("Failed to delete; Path not found; path=%s;", path)
            log.error { errorMessage }
            throw LocalStorageException(errorMessage)
        }
        try {
            log.info { String.format("Cleaning up files and directories recursively; paths=%s;", path.toString()) }
            Files.walk(path).use { walk ->
                walk.sorted(Comparator.reverseOrder())
                    .map { obj: Path -> obj.toFile() }
                    .forEach { obj: File -> obj.delete() }
            }
        } catch (e: IOException) {
            val errorMessage = String.format("Failed to delete; path=%s;", path.toString())
            throw LocalStorageException(errorMessage, e)
        }
    }

    fun write(filepath: Path, content: InputStream) {
        try {
            Files.copy(
                content,
                filepath,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: IOException) {
            val errorMessage = String.format("Failed to write to a file; path=%s;", filepath)
            throw LocalStorageException(errorMessage, e)
        }
    }

    fun write(filepath: Path, content: MultipartFile) {
        try {
            content.transferTo(filepath)
        } catch (e: IOException) {
            val errorMessage = String.format("Failed to write to a file; path=%s;", filepath)
            throw LocalStorageException(errorMessage, e)
        }
    }

    private fun checkFileExists(path: Path) {
        if (Files.exists(path)) {
            val errorMessage = String.format("File already exists; path=%s;", path)
            throw LocalStorageException(errorMessage)
        }
    }
}
