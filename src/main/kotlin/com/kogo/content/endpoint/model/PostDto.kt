package com.kogo.content.endpoint.model

import com.kogo.content.validator.ValidFile
import jakarta.validation.constraints.NotBlank
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

data class PostDto (
    @field:NotBlank
    var title: String,

    @field:NotBlank
    var content: String,

    @field:ValidFile(
        sizeLimit = 128000000, // 128MB
        acceptedMediaTypes = [MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE],
        message = "An image must have either 'image/png' or 'image/jpeg' media type and maximum size 128MB")
    var images: List<MultipartFile> = listOf(),

    @field:ValidFile(
        sizeLimit = 0,
        acceptedMediaTypes = ["video/mp4"],
        message = "A video must have either 'video/mp4' media type and maximum size 0MB")
    var videos: List<MultipartFile> = listOf(),
)
