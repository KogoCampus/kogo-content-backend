package com.kogo.content.validator

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileValidatorTest {

    private lateinit var fileValidator: FileValidator
    private lateinit var fileListValidator: FileListValidator
    private lateinit var context: ConstraintValidatorContext

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        fileValidator = FileValidator()
        fileListValidator = FileListValidator()
        context = mockk(relaxed = true)
    }

    @Test
    fun `should return true for valid file`() {
        val file = MockMultipartFile("file", "filename.jpg", "image/jpeg", ByteArray(5000000))
        fileValidator.initialize(ValidFile(sizeMax = 10000000, sizeMin = 1000000, acceptedMediaTypes = arrayOf("image/jpeg"), message = "Invalid file"))
        assertTrue { fileValidator.isValid(file, context) }
    }

    @Test
    fun `should return false for file size exceeding max limit`() {
        val file = MockMultipartFile("file", "filename.jpg", "image/jpeg", ByteArray(15000000))
        fileValidator.initialize(ValidFile(sizeMax = 10000000, sizeMin = 1000000, acceptedMediaTypes = arrayOf("image/jpeg"), message = "Invalid file"))
        assertFalse { fileValidator.isValid(file, context) }
        verify { context.buildConstraintViolationWithTemplate("File size exceeds limit of 10MB").addConstraintViolation() }
    }

    @Test
    fun `should return false for file size below min limit`() {
        val file = MockMultipartFile("file", "filename.jpg", "image/jpeg", ByteArray(500000))
        fileValidator.initialize(ValidFile(sizeMax = 10000000, sizeMin = 1000000, acceptedMediaTypes = arrayOf("image/jpeg"), message = "Invalid file"))
        assertFalse { fileValidator.isValid(file, context) }
        verify { context.buildConstraintViolationWithTemplate("File size smaller than minimum limit of 1MB").addConstraintViolation() }
    }

    @Test
    fun `should return false for unsupported media type`() {
        val file = MockMultipartFile("file", "filename.pdf", "application/pdf", ByteArray(5000000))
        fileValidator.initialize(ValidFile(sizeMax = 10000000, sizeMin = 1000000, acceptedMediaTypes = arrayOf("image/jpeg"), message = "Invalid file"))
        assertFalse { fileValidator.isValid(file, context) }
        verify { context.buildConstraintViolationWithTemplate("Unsupported media type: application/pdf").addConstraintViolation() }
    }

    @Test
    fun `should return true for valid file list`() {
        val file1 = MockMultipartFile("file1", "filename1.jpg", "image/jpeg", ByteArray(5000000))
        val file2 = MockMultipartFile("file2", "filename2.png", "image/png", ByteArray(2000000))
        fileListValidator.initialize(ValidFile(sizeMax = 10000000, sizeMin = 1000000, acceptedMediaTypes = arrayOf("image/jpeg", "image/png"), message = "Invalid file"))
        assertTrue { fileListValidator.isValid(listOf(file1, file2), context) }
    }

    @Test
    fun `should return false for invalid file list`() {
        val file1 = MockMultipartFile("file1", "filename1.jpg", "image/jpeg", ByteArray(15000000))
        val file2 = MockMultipartFile("file2", "filename2.pdf", "application/pdf", ByteArray(5000000))
        fileListValidator.initialize(ValidFile(sizeMax = 10000000, sizeMin = 1000000, acceptedMediaTypes = arrayOf("image/jpeg"), message = "Invalid file"))
        assertFalse { fileListValidator.isValid(listOf(file1, file2), context) }
    }

    @Test
    fun `should return true for empty or null file list`() {
        assertTrue { fileListValidator.isValid(emptyList(), context) }
        assertTrue { fileListValidator.isValid(null, context) }
    }
}
