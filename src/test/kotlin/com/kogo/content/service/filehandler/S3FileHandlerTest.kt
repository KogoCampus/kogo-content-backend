package com.kogo.content.service.filehandler

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import kotlin.test.assertTrue

class S3FileHandlerTest {
    private val bucketName = "test-bucket"

    @MockK
    private val s3Client: S3Client = mockk()

    @MockK
    private val s3Presigner: S3Presigner = mockk()

    private val S3FileHandler: S3FileHandler = S3FileHandler(s3Client, s3Presigner)

    @Test
    fun shouldStoreObject() {
        val content = "some-content"
        val file = MockMultipartFile("file", content.toByteArray())

        val filename = file.originalFilename

        val s3Object = S3Object(bucketName, filename, file)
        every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns mockk()

        val res = S3FileHandler.putS3Object(s3Object)

        verify { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) }

        assertTrue { res.fileName == filename }
    }
}
