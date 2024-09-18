package com.kogo.content.endpoint.model

import com.kogo.content.validator.ValidFile
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

data class UserUpdate (
    var username: String? = null,

    @field: ValidFile(
        sizeMax = 128000000, // 128MB
        acceptedMediaTypes = [MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE],
        message = "An image must have either 'image/png' or 'image/jpeg' media type and maximum size 128MB")
    var profileImage: MultipartFile?= null
)
