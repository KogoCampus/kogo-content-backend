package com.kogo.content.endpoint.model

import com.kogo.content.validator.ValidFile
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

data class PostUpdate (
    var title: String? = null,

    var content: String? = null,

    @ArraySchema(schema = Schema(description = "list of attachment ids to delete from the post", type = "String"))
    var attachmentDelete: List<String>? = listOf(),

    @ArraySchema(schema = Schema(description = "list of image files to add to the post", type = "File"))
    @field:ValidFile(
        sizeMax = 128000000, // 128MB
        acceptedMediaTypes = [MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE],
        message = "An image must have either 'image/png' or 'image/jpeg' media type and maximum size 128MB")
    var images: List<MultipartFile>? = listOf(),

    /**
     * Video attachment is currently not in use
     */
    @ArraySchema(schema = Schema(description = "list of video files to add to the post", type = "File"))
    @field:ValidFile(
        sizeMax = 1,
        acceptedMediaTypes = ["video/mp4"],
        message = "A video must have either 'video/mp4' media type and maximum size 0MB")
    var videos: List<MultipartFile>? = listOf(),
)
