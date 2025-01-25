package com.kogo.content.service.fileuploader

import com.kogo.content.exception.FileOperationFailure
import com.kogo.content.exception.FileOperationFailureException
import com.kogo.content.logging.Logger
import com.kogo.content.storage.model.Attachment
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile

open class FileUploaderService(){
    companion object : Logger()

    @Value("\${kogo-api.uploadFiles}")
    lateinit var fileUploaderUrl: String
    var restTemplate = RestTemplate()

    @PostConstruct
    fun checkConnection(){
        return try {
            val response = restTemplate.exchange(
                fileUploaderUrl,
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )
            if (response.statusCode.is2xxSuccessful) {
                val message = response.body!!["message"] as String
                log.info { "File Uploader Server connected: $message" }
            } else {
                throw FileOperationFailureException(FileOperationFailure.CONNECT, null, "Failed to connect to File Uploader Server, status code: ${response.statusCode}")
            }
        } catch (ex: Exception) {
            throw FileOperationFailureException(FileOperationFailure.CONNECT, null, "Failed to connect to File Uploader Server, status code: ${ex.message}")
        }
    }

    fun uploadImage(profileImage: MultipartFile): Attachment {
        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }
        val bodyBuilder = MultipartBodyBuilder().apply {
            part("file", profileImage.resource)
        }
        val requestEntity = HttpEntity(bodyBuilder.build(), headers)

        return try {
            val response = restTemplate.exchange(
                "${fileUploaderUrl}/images", // URL for file upload
                HttpMethod.POST,
                requestEntity,
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )

            // Check if the upload was successful
            if (response.statusCode.is2xxSuccessful) {
                log.info { "Profile image uploaded successfully" }

                // Get the response body and create the Attachment object
                val responseBody = response.body ?: throw FileOperationFailureException(FileOperationFailure.UPLOAD, profileImage.contentType!!, "Empty response body")

                val imageId = responseBody["imageId"] as String
                val filename = responseBody["filename"] as String
                val url = responseBody["url"] as String
                val contentType = responseBody["content_type"] as String
                val size = (responseBody["size"] as Number).toLong()

                // Return the Attachment object with extracted values
                Attachment(
                    id = imageId,
                    filename = filename,
                    url = url,
                    contentType = contentType,
                    size = size
                )
            } else {
                throw FileOperationFailureException(FileOperationFailure.UPLOAD, profileImage.contentType!!, "Failed to upload image, status code: ${response.statusCode}")
            }
        } catch (ex: Exception) {
            throw FileOperationFailureException(FileOperationFailure.UPLOAD, profileImage.contentType!!, ex.toString())
        }
    }

    fun deleteImage(imageId: String){
        try{
            val response = restTemplate.exchange(
                "$fileUploaderUrl/images/$imageId", // URL for file delete
                HttpMethod.DELETE,
                null, // No body or headers required
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )
            if (response.statusCode.is2xxSuccessful) {
                log.info { "Successfully deleted image with ID: $imageId" }
            } else {
                throw FileOperationFailureException(FileOperationFailure.DELETE, null, "Failed to delete image with ID: $imageId, status code: ${response.statusCode}")
            }
        } catch (ex: Exception) {
            throw FileOperationFailureException(FileOperationFailure.DELETE, null, "Error while deleting image with ID: $imageId: ${ex.message}")
        }
    }
}
