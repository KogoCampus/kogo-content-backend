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
            "imageId" to "test-image-id",
            "filename" to "test.jpg",
            "url" to "http://test-url/test.jpg",
            "content_type" to MediaType.IMAGE_JPEG_VALUE,
            "size" to 1024L
        )

        every {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/images",
                HttpMethod.POST,
                any(),
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        } returns ResponseEntity(expectedResponse, HttpStatus.OK)

        // When
        val result = fileUploaderService.uploadImage(imageFile)

        // Then
        assertThat(result.id).isEqualTo(expectedResponse["imageId"])
        assertThat(result.filename).isEqualTo(expectedResponse["filename"])
        assertThat(result.url).isEqualTo(expectedResponse["url"])
        assertThat(result.contentType).isEqualTo(expectedResponse["content_type"])
        assertThat(result.size).isEqualTo(expectedResponse["size"])

        verify {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/images",
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
            fileUploaderService.uploadImage(imageFile)
        }
    }

    @Test
    fun `should delete image successfully`() {
        // Given
        val imageId = "test-image-id"
        val expectedResponse = mapOf<String, Any>()

        every {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/images/$imageId",
                HttpMethod.DELETE,
                null,
                any<ParameterizedTypeReference<Map<String, Any>>>()
            )
        } returns ResponseEntity(expectedResponse, HttpStatus.OK)

        // When & Then
        fileUploaderService.deleteImage(imageId)

        verify {
            restTemplate.exchange(
                "${fileUploaderService.fileUploaderUrl}/images/$imageId",
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
            fileUploaderService.deleteImage(imageId)
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
            fileUploaderService.uploadImage(imageFile)
        }
    }
}
