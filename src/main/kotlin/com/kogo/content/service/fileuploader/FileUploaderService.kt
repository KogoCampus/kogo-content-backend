package com.kogo.content.service.fileuploader

import com.kogo.content.exception.FileOperationFailure
import com.kogo.content.exception.FileOperationFailureException
import com.kogo.content.logging.Logger
import com.kogo.content.storage.model.Attachment
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.multipart.MultipartFile

open class FileUploaderService(){
    companion object : Logger()

    @Value("\${kogo-api.uploadFiles}")
    lateinit var fileUploaderUrl: String
    var restTemplate = RestTemplate()

    fun uploadImage(image: MultipartFile): Attachment {
        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }
        val bodyBuilder = MultipartBodyBuilder().apply {
            part("file", image.resource)
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
                log.info { "Image uploaded successfully" }

                createAttachment(response)
            } else {
                throw FileOperationFailureException(FileOperationFailure.UPLOAD, image.contentType!!, "Failed to upload image, status code: ${response.statusCode}")
            }
        } catch (ex: Exception) {
            throw FileOperationFailureException(FileOperationFailure.UPLOAD, image.contentType!!, ex.toString())
        }
    }

    fun staleImage(image: MultipartFile): Attachment {
        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }
        val bodyBuilder = MultipartBodyBuilder().apply {
            part("file", image.resource)
        }
        val requestEntity = HttpEntity(bodyBuilder.build(), headers)

        return try {
            val response = restTemplate.exchange(
                "${fileUploaderUrl}/schedules", // URL for file upload
                HttpMethod.POST,
                requestEntity,
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )

            // Check if the upload was successful
            if (response.statusCode.is2xxSuccessful) {
                log.info { "Image uploaded successfully" }
                println(response)
                createAttachment(response)
            } else {
                throw FileOperationFailureException(FileOperationFailure.UPLOAD, image.contentType!!, "Failed to upload image, status code: ${response.statusCode}")
            }
        } catch (ex: Exception) {
            throw FileOperationFailureException(FileOperationFailure.UPLOAD, image.contentType!!, ex.toString())
        }
    }

    fun persistImage(imageId: String): Attachment{
        return try {
            restTemplate.exchange(
                "${fileUploaderUrl}/schedules/keep/${imageId}", // URL for file staling
                HttpMethod.POST,
                null,
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )
            val response = restTemplate.exchange(
                "${fileUploaderUrl}/images/${imageId}", // URL for file staling
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

    private fun createAttachment(response: ResponseEntity<Map<String, Any>>, isPersisted: Boolean=false): Attachment{
        val responseBody = response.body!!

        val imageId = responseBody["file_id"] as String
        val filename = responseBody["filename"] as String
        val url = responseBody["url"] as String

        val metadata = responseBody["metadata"] as? Map<*, *>

        val contentType: String
        val finalSize: Long

        if (metadata != null) {
            contentType = metadata["content_type"] as String
            finalSize = (metadata["size"] as Number).toLong()
        } else {
            contentType = responseBody["content_type"] as String
            finalSize = (responseBody["size"] as Number).toLong()
        }

        // Return the Attachment object with extracted values
        return Attachment(
            id = imageId,
            filename = filename,
            url = url,
            contentType = contentType,
            size = finalSize,
            isPersisted = isPersisted
        )
    }
}
