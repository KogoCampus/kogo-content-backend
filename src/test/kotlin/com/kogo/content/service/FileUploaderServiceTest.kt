package com.kogo.content.service

import com.kogo.content.exception.FileOperationFailureException
import com.kogo.content.service.fileuploader.FileUploaderService
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.client.RestTemplate
import com.kogo.content.storage.model.Attachment

class FileUploaderServiceTest {
    private val restTemplate: RestTemplate = mockk()
    private lateinit var fileUploaderService: FileUploaderService

    @BeforeEach
    fun setup() {
        fileUploaderService = FileUploaderService().apply {
            restTemplate = this@FileUploaderServiceTest.restTemplate
            fileUploaderUrl = "http://test-uploader"
        }
    }

    @Test
    fun `should upload image successfully`() {
        // Given
        val imageFile = MockMultipartFile(
            "test-image",
            "test.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".toByteArray()
        )

        val expectedResponse = mapOf(
            "file_id" to "test-image-id",
            "filename" to "test.jpg",
            "url" to "http://test-url/test.jpg",
            "metadata" to mapOf(
                "content_type" to MediaType.IMAGE_JPEG_VALUE,
                "size" to 1024L
            )
        )

        every {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/files",
                HttpMethod.POST,
                any(),
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        } returns ResponseEntity(expectedResponse, HttpStatus.OK)

        // When
        val result = fileUploaderService.uploadFile(imageFile)

        // Then
        assertThat(result.id).isEqualTo(expectedResponse["file_id"])
        assertThat(result.filename).isEqualTo(expectedResponse["filename"])
        assertThat(result.url).isEqualTo(expectedResponse["url"])
        // Extract metadata
        val metadata = expectedResponse["metadata"] as Map<String, Any>
        // Validate metadata fields
        assertThat(result.contentType).isEqualTo(metadata["content_type"])
        assertThat(result.size).isEqualTo(metadata["size"])

        verify {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/files",
                HttpMethod.POST,
                any(),
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        }
    }

    @Test
    fun `should throw exception when upload fails with non-2xx status`() {
        // Given
        val imageFile = MockMultipartFile(
            "test-image",
            "test.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".toByteArray()
        )

        every {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/images",
                HttpMethod.POST,
                any(),
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        } returns ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)

        // When & Then
        assertThrows<FileOperationFailureException> {
            fileUploaderService.uploadFile(imageFile)
        }
    }

    @Test
    fun `should stale image successfully`(){
        // Given
        val imageFile = MockMultipartFile(
            "test-image",
            "test.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".toByteArray()
        )

        val expectedResponse = mapOf(
            "file_id" to "test-image-id",
            "filename" to "test.jpg",
            "url" to "http://test-url/test.jpg",
            "metadata" to mapOf(
                "content_type" to MediaType.IMAGE_JPEG_VALUE,
                "size" to 1024L
            )
        )

        every {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/stale",
                HttpMethod.POST,
                any(),
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        } returns ResponseEntity(expectedResponse, HttpStatus.OK)

        // When
        val result = fileUploaderService.staleFile(imageFile)

        // Then
        assertThat(result.id).isEqualTo(expectedResponse["file_id"])
        assertThat(result.filename).isEqualTo(expectedResponse["filename"])
        assertThat(result.url).isEqualTo(expectedResponse["url"])
        // Extract metadata
                val metadata = expectedResponse["metadata"] as Map<String, Any>
        // Validate metadata fields
                assertThat(result.contentType).isEqualTo(metadata["content_type"])
                assertThat(result.size).isEqualTo(metadata["size"])

        verify {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/stale",
                HttpMethod.POST,
                any(),
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        }
    }

    @Test
    fun `should delete file successfully`() {
        // Given
        val fileId = "test-filee-id"
        val expectedResponse = mapOf<String, Any>()

        every {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/files/$fileId",
                HttpMethod.DELETE,
                null,
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        } returns ResponseEntity(expectedResponse, HttpStatus.OK)

        // When & Then
        fileUploaderService.deleteFile(fileId)

        verify {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/files/$fileId",
                HttpMethod.DELETE,
                null,
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        }
    }

    @Test
    fun `should throw exception when delete fails with non-2xx status`() {
        // Given
        val imageId = "test-image-id"

        every {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/images/$imageId",
                HttpMethod.DELETE,
                null,
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        } returns ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)

        // When & Then
        assertThrows<FileOperationFailureException> {
            fileUploaderService.deleteFile(imageId)
        }
    }

    @Test
    fun `should throw exception when upload response body is null`() {
        // Given
        val imageFile = MockMultipartFile(
            "test-image",
            "test.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".toByteArray()
        )

        every {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/images",
                HttpMethod.POST,
                any(),
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        } returns ResponseEntity(null, HttpStatus.OK)

        // When & Then
        assertThrows<FileOperationFailureException> {
            fileUploaderService.uploadFile(imageFile)
        }
    }
}
