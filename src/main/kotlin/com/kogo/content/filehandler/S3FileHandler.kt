package com.kogo.content.filehandler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.ByteArrayInputStream
import java.util.*

data class S3Object (
    val bucket: String,
    val filename: String,
    val content: MultipartFile
)

@Profile("!local & !test")
@Component
class S3FileHandler() : FileHandler {

    @Autowired
    private lateinit var s3Client: S3Client

    @Autowired
    private lateinit var s3Presigner: S3Presigner

    @Value("\${aws.s3.bucket}")
    private lateinit var bucketName: String

    @Value("\${aws.s3.region}")
    private lateinit var region: String

    constructor(s3Client: S3Client, s3Presigner: S3Presigner) : this() {
        this.s3Client = s3Client
        this.s3Presigner = s3Presigner
    }

    override fun store(content: MultipartFile): FileStoreMetadata {
        val s3Object = S3Object(bucketName, content.originalFilename!!, content)
        return putS3Object(s3Object)
    }

    fun putS3Object(s3Object: S3Object): FileStoreMetadata {
        val objectKey = createS3ObjectKey(s3Object.filename)
        val bytes = s3Object.content.bytes
        val inputStream = ByteArrayInputStream(bytes)
        val objectRequest = PutObjectRequest.builder()
            .bucket(s3Object.bucket)
            .key(objectKey)
            .contentType(s3Object.content.contentType)
            .contentDisposition("inline")
            .build()
        s3Client.putObject(objectRequest, RequestBody.fromInputStream(inputStream, s3Object.content.size))

        return FileStoreMetadata(
            fileName = s3Object.filename,
            storeKey = FileStoreKey(objectKey)
        )
    }

    // generates a presigned url to download
    override fun issueFilePublicSourceUrl(storeKey: FileStoreKey): String {
        return "https://${bucketName}.s3.${region}.amazonaws.com/${storeKey.key}"
    }

    private fun createS3ObjectKey(fileName: String): String {
        return String.format("%s-%s", UUID.randomUUID().toString(), fileName)
    }
}
