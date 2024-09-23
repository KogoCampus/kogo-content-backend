package com.kogo.content.filehandler

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.regions.Region
import jakarta.annotation.PostConstruct
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.File
import java.io.FileOutputStream
import java.util.*

// 1. send file info and get presigned url
// 2. store a file into the bucket
// 3. check if api return url is correct


@Profile("stg")
@Component
class S3FileHandler() : FileHandler{

    lateinit var s3Client: S3Client

    @Value("\${aws.s3.bucket}")
    lateinit var bucketName: String

    @Value("\${cloud.aws.region.static}")
    lateinit var region: String

    @Value("\${cloud.aws.credentials.accessKey}")
    lateinit var accessKey: String

    @Value("\${cloud.aws.credentials.secretKey}")
    lateinit var secretKey: String

    constructor(s3Client: S3Client) : this() {
        this.s3Client = s3Client
    }
//    @PostConstruct
//    fun init() {
//        val awsCreds = AwsBasicCredentials.create(accessKey, secretKey)
//        s3Client = S3Client.builder()
//            .region(Region.of(region))
//            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
//            .build()
//    }

    override fun store(content: MultipartFile): FileStoreMetadata {
        val name = fileStoreName(content.originalFilename!!)
        val objectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(name)
            .build()
        val fileContent = RequestBody.fromFile(multipartToFile(content))
        s3Client.putObject(objectRequest, fileContent)

        return FileStoreMetadata(
            fileName = content.originalFilename!!,
            storeKey = FileStoreKey(name)
        )
    }
    override fun issueFilePublicSourceUrl(storeKey: FileStoreKey): String {
        // getPresignedUrl
        return "asd"
    }

    private fun generatePresignedUrl(objectKey: String) {
        val presigner = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .build()
    }

    private fun fileStoreName(fileName: String): String {
        return String.format("%s-%s", UUID.randomUUID().toString(), fileName)
    }

    private fun multipartToFile(content: MultipartFile): File {
        val file = File(content.originalFilename!!)
        val outputStream = FileOutputStream(file)
        outputStream.write(file.readBytes());
        outputStream.close();
        return file
    }
}
