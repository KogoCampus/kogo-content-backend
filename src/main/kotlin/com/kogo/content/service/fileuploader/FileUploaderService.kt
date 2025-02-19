package com.kogo.content.service.fileuploader

import com.kogo.content.exception.FileOperationFailure
import com.kogo.content.exception.FileOperationFailureException
import com.kogo.content.logging.Logger
import com.kogo.content.storage.model.Attachment
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import java.util.*

open class FileUploaderService(){
    companion object : Logger()

    @Value("\${kogo-api.uploadFiles}")
    lateinit var fileUploaderUrl: String
    var restTemplate = RestTemplate()

    @Value("\${security.secret-key}")
    private lateinit var secretKey: String

    fun uploadFile(file: MultipartFile): Attachment {
        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }
        val bodyBuilder = MultipartBodyBuilder().apply {
            part("file", file.resource)
        }
        val requestEntity = HttpEntity(bodyBuilder.build(), headers)

        return try {
            val response = restTemplate.exchange(
                "${fileUploaderUrl}/files", // URL for file upload
                HttpMethod.POST,
                requestEntity,
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )

            // Check if the upload was successful
            if (response.statusCode.is2xxSuccessful) {
                createAttachment(response, true)
            } else {
                throw FileOperationFailureException(FileOperationFailure.UPLOAD, file.contentType!!, "Failed to upload a file, status code: ${response.statusCode}")
            }
        } catch (ex: Exception) {
            throw FileOperationFailureException(FileOperationFailure.UPLOAD, file.contentType!!, ex.toString())
        }
    }

    fun staleFile(file: MultipartFile): Attachment {
        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }
        val bodyBuilder = MultipartBodyBuilder().apply {
            part("file", file.resource)
        }
        val requestEntity = HttpEntity(bodyBuilder.build(), headers)

        return try {
            val response = restTemplate.exchange(
                "${fileUploaderUrl}/stale", // URL for file upload
                HttpMethod.POST,
                requestEntity,
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )

            // Check if the upload was successful
            if (response.statusCode.is2xxSuccessful) {
                createAttachment(response)
            } else {
                throw FileOperationFailureException(FileOperationFailure.UPLOAD, file.contentType!!, "Failed to upload a file, status code: ${response.statusCode}")
            }
        } catch (ex: Exception) {
            throw FileOperationFailureException(FileOperationFailure.UPLOAD, file.contentType!!, ex.toString())
        }
    }

    fun persistFile(fileId: String): Attachment{
        return try {
            restTemplate.exchange(
                "${fileUploaderUrl}/stale/persist/${fileId}", // URL for file staling
                HttpMethod.POST,
                null,
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )
            val response = restTemplate.exchange(
                "${fileUploaderUrl}/files/${fileId}", // URL for file staling
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )
            if (response.statusCode.is2xxSuccessful) {
                createAttachment(response, true)
            } else throw FileOperationFailureException(FileOperationFailure.KEEP, null, "Failed to keep")
        } catch (ex: Exception){
            throw FileOperationFailureException(FileOperationFailure.KEEP, null, ex.toString())
        }
    }

    fun deleteFile(fileId: String){
        try{
            val response = restTemplate.exchange(
                "$fileUploaderUrl/files/$fileId", // URL for file delete
                HttpMethod.DELETE,
                null, // No body or headers required
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )
            if (response.statusCode.is2xxSuccessful) {
                log.info { "Successfully deleted a file with ID: $fileId" }
            } else {
                throw FileOperationFailureException(FileOperationFailure.DELETE, null, "Failed to delete a file with ID: $fileId, status code: ${response.statusCode}")
            }
        } catch (ex: Exception) {
            throw FileOperationFailureException(FileOperationFailure.DELETE, null, "Error while deleting a file with ID: $fileId: ${ex.message}")
        }
    }

    private fun createAttachment(response: ResponseEntity<Map<String, Any>>, isPersisted: Boolean=false): Attachment{
        val responseBody = response.body!!

        val file_id = responseBody["file_id"] as String
        val filename = responseBody["filename"] as String
        val url = responseBody["url"] as String

        val metadata = responseBody["metadata"] as Map<*, *>

        val contentType = metadata["content_type"] as String
        val finalSize = (metadata["size"] as Number).toLong()

        // Return the Attachment object with extracted values
        return Attachment(
            id = file_id,
            filename = filename,
            url = url,
            contentType = contentType,
            size = finalSize,
            isPersisted = isPersisted
        )
    }

    fun createFileToken(file: Attachment): String {
        val algorithm = Algorithm.HMAC256(secretKey)

        return JWT.create()
            .withSubject("file_token")
            .withClaim("file_id", file.id)
            .withClaim("filename", file.filename)
            .withClaim("url", file.url)
            .withClaim("contentType", file.contentType)
            .withClaim("size", file.size)
            .withExpiresAt(Date(System.currentTimeMillis() + 20 * 60 * 1000)) // Expires in 20 mins
            .sign(algorithm)
    }

    fun decodeFileToken(fileToken: String): String {
        val decodedJWT = JWT.decode(fileToken)
        return decodedJWT.getClaim("file_id").asString()
    }
}
