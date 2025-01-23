package com.kogo.content.endpoint.model

import com.kogo.content.endpoint.validator.ValidFile
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

class FileUploaderDto {

    data class Image(
        @field: ValidFile(
            sizeMax = 12000000, // 12MB
            acceptedMediaTypes = [MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE],
            message = "An image must have either 'image/png' or 'image/jpeg' media type and maximum size 12MB")
        val image: MultipartFile
    )
}
